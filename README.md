# RealTeeth Backend Assignment

Spring Boot + Kotlin 기반으로 외부 AI 이미지 처리 시스템을 안정적으로 연동하는 백엔드 서버를 설계하고 구현하는 과제입니다.

## 기술 스택

- **Language**: Kotlin 2.2
- **Framework**: Spring Boot 3.4.5
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA / Hibernate
- **Build**: Gradle (Kotlin DSL)
- **Container**: Docker / Docker Compose

---

## 실행 방법

### 1단계 — Mock Worker API Key 발급

```bash
curl -X POST https://dev.realteeth.ai/mock/auth/issue-key \
  -H "Content-Type: application/json" \
  -d '{"candidateName": "홍길동", "email": "example@email.com"}'

# 응답 예시
# { "apiKey": "mock_xxxxxxxxxxxxxxxx" }
```

발급받은 `apiKey` 값을 이후 실행 시 환경변수로 사용합니다.

---

### Docker Compose로 실행

MySQL 컨테이너와 애플리케이션을 함께 실행합니다.

```bash
# 1. JAR 빌드
./gradlew build -x test

# 2. 실행
MOCK_WORKER_API_KEY=mock_xxx docker compose up --build
```

서버: `http://localhost:8080`
MySQL: `localhost:3306` (user: `realteeth`, password: `realteeth`, database: `realteeth`)

---

### 로컬 실행

로컬 MySQL이 준비된 경우:

```bash
# DB 생성
mysql -u root -p -e "
  CREATE DATABASE IF NOT EXISTS realteeth;
  CREATE USER IF NOT EXISTS 'realteeth'@'localhost' IDENTIFIED BY 'realteeth';
  GRANT ALL PRIVILEGES ON realteeth.* TO 'realteeth'@'localhost';
  FLUSH PRIVILEGES;
"

# 빌드 및 실행
./gradlew build -x test

MOCK_WORKER_API_KEY=mock_xxx \
DB_HOST=localhost \
DB_NAME=realteeth \
DB_USERNAME=realteeth \
DB_PASSWORD=realteeth \
java -jar build/libs/realteeth-task-0.0.1-SNAPSHOT.jar
```

---

### 테스트 실행

```bash
./gradlew test
```

---

## API 명세

### 포트

| 서비스 | 포트 |
|--------|------|
| 애플리케이션 | 8080 |
| MySQL | 3306 |

### 엔드포인트

| Method | URL | 설명 | 응답 코드 |
|--------|-----|------|-----------|
| `POST` | `/api/jobs` | 이미지 처리 작업 요청 | 202 |
| `GET` | `/api/jobs/{id}` | 작업 상태 및 결과 조회 | 200 / 404 |
| `GET` | `/api/jobs` | 작업 목록 조회 | 200 |

---

### POST `/api/jobs` — 작업 생성

동일한 `imageUrl`로 이미 처리 중인(PENDING / PROCESSING) 또는 완료된(COMPLETED) 작업이 있으면 기존 작업을 반환합니다. 실패한(FAILED) 작업만 있는 경우 새 작업을 생성합니다.

**Request**
```json
{
  "imageUrl": "https://example.com/image.jpg"
}
```

**Response `202 Accepted`**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "imageUrl": "https://example.com/image.jpg",
  "status": "PENDING",
  "createdAt": "2026-03-10T12:00:00"
}
```

**Error Cases**
| 코드 | 원인 |
|------|------|
| 400 | imageUrl이 비어있거나 http/https 형식이 아닌 경우 |

---

### GET `/api/jobs/{id}` — 작업 조회

**Response `200 OK`**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "imageUrl": "https://example.com/image.jpg",
  "status": "COMPLETED",
  "result": "처리 결과 데이터",
  "failureReason": null,
  "createdAt": "2026-03-10T12:00:00",
  "updatedAt": "2026-03-10T12:00:45"
}
```

- `status`가 `FAILED`인 경우 `failureReason`에 실패 원인이 포함됩니다.
- `status`가 `COMPLETED`가 아닌 경우 `result`는 `null`입니다.

**Error Cases**
| 코드 | 원인 |
|------|------|
| 404 | 해당 id의 작업이 존재하지 않는 경우 |

---

### GET `/api/jobs` — 작업 목록 조회

**Response `200 OK`**
```json
{
  "jobs": [ ... ],
  "total": 10
}
```

---

## 설계 설명

### 상태 모델 설계 의도

```
           ┌─────────────────┐
           │                 │
  POST /api/jobs         재시도
           │                 │
           ▼                 │
        PENDING ──────────► FAILED
           │
           │  JobSubmitter (5초 주기)
           │  Mock Worker 제출 성공
           ▼
        PROCESSING ────────► FAILED
           │         타임아웃 / 워커 실패
           │
           │  JobPollingScheduler (10초 주기)
           │  Mock Worker 완료 감지
           ▼
        COMPLETED
```

| 상태 | 의미 | Terminal |
|------|------|----------|
| `PENDING` | 작업 접수, Mock Worker 제출 대기 | ✗ |
| `PROCESSING` | Mock Worker에 제출 완료, 결과 대기 | ✗ |
| `COMPLETED` | 처리 성공, `result` 필드에 결과 포함 | ✓ |
| `FAILED` | 처리 실패, `failureReason` 필드에 원인 포함 | ✓ |

**허용되지 않는 전이:** `COMPLETED → *`, `FAILED → *`, `PROCESSING → PENDING`, `COMPLETED → PROCESSING` 등 역방향 전이는 모두 차단됩니다.

상태 전이 규칙은 `JobStatus.canTransitionTo()`에 캡슐화되어 있으며, `Job.transitionTo()`를 통해서만 상태를 변경할 수 있습니다. `UpdateJobStatusServiceImpl`에서는 이미 terminal 상태인 job에 대한 전이 요청을 예외 없이 무시하여, 스케줄러 중복 실행이나 재시작 시에도 안전하게 동작합니다.

---

### 외부 시스템 연동 방식 및 선택 이유

**Polling(Pull) 방식**을 채택했습니다.

Mock Worker는 webhook을 제공하지 않으므로 서버가 주기적으로 상태를 조회합니다. 두 개의 스케줄러가 역할을 분리하여 담당합니다.

```
┌─────────────────────────────────────────────────────────┐
│  JobSubmitter (fixedDelay: 5s)                          │
│                                                         │
│  PENDING jobs → POST /mock/process → PROCESSING         │
│              → 429: PENDING 유지, 다음 사이클 재시도     │
│              → 5xx: FAILED 처리                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  JobPollingScheduler (fixedDelay: 10s)                  │
│                                                         │
│  PROCESSING jobs → GET /mock/process/{jobId}            │
│                  → COMPLETED: DB 업데이트               │
│                  → FAILED: DB 업데이트                  │
│                  → PROCESSING: 다음 사이클 재시도        │
│                  → 타임아웃(30분): FAILED 처리           │
│                  → 5xx/네트워크 오류: 로그 후 다음 사이클 │
└─────────────────────────────────────────────────────────┘
```

`@Scheduled(fixedDelay)`를 사용하여 이전 실행이 완전히 끝난 후에만 다음 실행이 시작됩니다. 처리가 지연되더라도 스케줄러가 겹쳐 실행되지 않습니다.

HTTP 클라이언트로 Spring 6.1의 `RestClient`를 사용했습니다. WebFlux 의존성 없이 사용 가능하고, Mock Worker 연동은 동기 I/O로 충분하기 때문입니다.

---

### 실패 처리 전략

| 상황 | 처리 방식 | 이유 |
|------|-----------|------|
| Mock Worker 429 (제출 시) | PENDING 유지, 다음 사이클 재시도 | 일시적 과부하이므로 재시도가 적합 |
| Mock Worker 4xx/5xx (제출 시) | FAILED 처리 | 클라이언트 오류나 서버 장애는 즉시 실패 처리 |
| Mock Worker 5xx / 네트워크 오류 (폴링 시) | 로그 기록, 다음 사이클 재시도 | 폴링 오류는 일시적 장애일 가능성이 높아 상태 유지 |
| Mock Worker FAILED 반환 | FAILED 처리 | 워커가 명시적으로 실패를 선언한 경우 |
| PROCESSING 타임아웃 (기본 30분) | FAILED 처리, 사유 기록 | 무한 폴링 방지 (`job.updatedAt` 기준으로 경과 시간 측정) |

타임아웃 임계값은 `application.yml`의 `job.processing-timeout-minutes`로 조정할 수 있습니다.

---

### 중복 요청 처리 전략

`imageUrl`을 idempotency key로 사용합니다.

```
동일 imageUrl 요청
        │
        ▼
PENDING / PROCESSING / COMPLETED 존재? ──── YES ──► 기존 job 반환
        │
        NO
        │
        ▼
FAILED 존재 또는 최초 요청 ──────────────────────► 새 job 생성
```

**동시 중복 요청 방어:**

`CreateJobServiceImpl`에 `Isolation.SERIALIZABLE`을 적용했습니다.

MySQL InnoDB는 SERIALIZABLE 격리 수준에서 `SELECT` 시 gap lock을 획득합니다. 두 트랜잭션이 동시에 동일 `imageUrl`로 INSERT를 시도하면 gap lock 충돌로 하나는 데드락으로 롤백됩니다. 롤백된 요청의 클라이언트가 재시도하면 이미 생성된 job을 반환받습니다.

실제 운영 환경(다중 인스턴스)에서는 Redis 기반 분산 락으로 대체하는 것이 더 안정적입니다.

---

### 처리 보장 모델

**At-least-once (최소 한 번 처리 보장)**에 해당합니다.

- PENDING job은 서버가 재시작되어도 `JobSubmitter` 다음 실행 시 자동으로 재제출됩니다.
- PROCESSING job은 재시작 후 `workerJobId`를 기반으로 폴링이 재개됩니다.
- Mock Worker 429 응답 시 다음 사이클에 재시도하므로 제출은 최소 한 번 이상 보장됩니다.

Exactly-once를 보장하지 않는 이유는, 서버 재시작 시점에 따라 Mock Worker에 동일 작업이 중복 제출될 수 있기 때문입니다. 이는 `UpdateJobStatusServiceImpl`에서 terminal 상태 job의 전이를 무시함으로써 멱등하게 처리합니다.

---

### 서버 재시작 시 동작

| 재시작 전 상태 | 재시작 후 동작 |
|--------------|--------------|
| `PENDING` | `JobSubmitter` 다음 사이클에 자동 재제출 |
| `PROCESSING` | `JobPollingScheduler`가 `workerJobId` 기반으로 폴링 재개 |
| `COMPLETED` / `FAILED` | 영향 없음 (terminal 상태) |

**데이터 정합성이 깨질 수 있는 지점:**

1. **제출 직후 재시작**: Mock Worker에 제출은 성공했으나, DB에 `PROCESSING` 저장 전에 서버가 재시작된 경우
   - job은 `PENDING` 상태를 유지
   - `JobSubmitter`가 다음 사이클에 동일 `imageUrl`로 Mock Worker에 재제출
   - Mock Worker에 중복 작업이 생성될 수 있음
   - 완화 방안: 제출과 DB 업데이트를 같은 로컬 트랜잭션으로 묶는 것은 불가능(외부 API), 운영 환경에서는 outbox 패턴 적용 고려

2. **폴링 응답 직후 재시작**: Mock Worker로부터 `COMPLETED` 응답을 수신했으나, DB 업데이트 전에 서버가 재시작된 경우
   - 다음 폴링 시 Mock Worker에서 해당 `workerJobId`를 찾지 못하면 (404) Error 로그만 기록되어 `PROCESSING` 상태가 유지될 수 있음
   - 이 경우 타임아웃(30분)이 지나면 자동으로 `FAILED` 처리됨

---

### 동시 요청 고려 사항

- **DB 레벨 직렬화**: `UpdateJobStatusServiceImpl`은 `PESSIMISTIC_WRITE` 락을 사용하여 동일 job에 대한 동시 상태 변경을 직렬화합니다. 두 스케줄러 또는 두 인스턴스가 동시에 같은 job을 업데이트하려 해도 하나가 끝난 후 다른 하나가 처리됩니다.

- **스케줄러 단일 스레드**: `@Scheduled(fixedDelay)`는 기본적으로 단일 스레드로 동작하므로 단일 인스턴스에서는 스케줄러 내부 동시성 문제가 없습니다.

- **수평 확장 시**: 여러 인스턴스가 동일 PENDING/PROCESSING job을 동시에 처리할 수 있습니다. `PESSIMISTIC_WRITE` 락이 1차 방어선이 되며, `canTransitionTo()` 체크와 terminal 상태 무시 처리가 2차 방어선이 됩니다. 완전한 해결을 위해서는 분산 락(Redis) 또는 메시지 큐(Kafka, SQS) 도입이 필요합니다.

---

### 트래픽 증가 시 병목 가능 지점

| 지점 | 병목 원인 | 개선 방안 |
|------|-----------|-----------|
| `JobPollingScheduler` | PROCESSING job이 증가하면 폴링 횟수 선형 증가, Mock Worker 429 빈도 상승 | Mock Worker가 batch 조회를 지원한다면 일괄 폴링으로 개선. 또는 폴링 간격을 동적으로 조정 |
| `findAllByStatus()` | PROCESSING job이 수만 건이면 한 사이클 내 처리량 초과, 스케줄러 지연 발생 | `status` 컬럼 인덱스 추가, 페이지 단위로 나눠 처리 |
| MySQL SERIALIZABLE | 동시 요청 증가 시 데드락 빈도 상승, 커넥션 풀 고갈 가능 | Redis 분산 락으로 대체, 또는 DB 커넥션 풀 크기 조정 |
| `PESSIMISTIC_WRITE` 락 | 다중 인스턴스에서 같은 job에 대한 락 경합 발생 | 인스턴스별 job 파티셔닝 (예: `id % n == instanceId`) 또는 메시지 큐 도입 |

---

## 프로젝트 구조

```
src/main/kotlin/com/seungchan/realteeth/
├── RealteethTaskApplication.kt
├── domain/job/
│   ├── client/
│   │   ├── MockWorkerClient.kt          # Mock Worker HTTP 연동 (submit / poll)
│   │   ├── constant/MockJobStatus.kt
│   │   └── dto/
│   │       ├── request/MockProcessRequest.kt
│   │       └── response/MockProcessStartResponse.kt, MockProcessStatusResponse.kt
│   ├── entity/
│   │   ├── Job.kt                       # JPA 엔티티, transitionTo() 포함
│   │   └── constant/JobStatus.kt        # 상태 enum + canTransitionTo() 상태기계
│   ├── presentation/
│   │   ├── JobController.kt
│   │   └── data/
│   │       ├── request/CreateJobRequest.kt
│   │       └── response/CreateJobResponse.kt, GetJobResponse.kt, GetJobListResponse.kt
│   ├── repository/JobRepository.kt      # JPA Repository + 비관적 락 쿼리
│   ├── scheduler/
│   │   ├── JobSubmitter.kt              # PENDING → Mock Worker 제출 (5초 주기)
│   │   └── JobPollingScheduler.kt       # PROCESSING 상태 폴링 (10초 주기)
│   └── service/
│       ├── CreateJobService.kt
│       ├── GetJobService.kt
│       ├── GetJobListService.kt
│       ├── FindActiveJobsService.kt
│       ├── UpdateJobStatusService.kt
│       ├── command/UpdateJobStatusCommand.kt
│       └── impl/
│           ├── CreateJobServiceImpl.kt
│           ├── GetJobServiceImpl.kt
│           ├── GetJobListServiceImpl.kt
│           ├── FindActiveJobsServiceImpl.kt
│           └── UpdateJobStatusServiceImpl.kt
└── global/
    ├── config/
    │   ├── JobProperties.kt             # job.processing-timeout-minutes
    │   ├── MockWorkerProperties.kt      # mock-worker.base-url, api-key
    │   └── RestClientConfig.kt
    └── error/
        ├── ErrorCode.kt
        ├── GlobalExceptionHandler.kt
        └── exception/
            ├── RealteethException.kt    # 베이스 커스텀 예외
            ├── JobNotFoundException.kt
            ├── InvalidJobStateTransitionException.kt
            └── InvalidImageUrlException.kt
```
