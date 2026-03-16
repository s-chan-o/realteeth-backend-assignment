package com.seungchan.realteeth.domain.job.integration

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.presentation.data.request.CreateJobRequest
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.CreateJobService
import com.seungchan.realteeth.domain.job.service.GetJobService
import com.seungchan.realteeth.domain.job.service.UpdateJobStatusService
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import com.seungchan.realteeth.global.error.exception.JobNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class JobIntegrationTest {

    @Autowired
    private lateinit var createJobService: CreateJobService

    @Autowired
    private lateinit var getJobService: GetJobService

    @Autowired
    private lateinit var updateJobStatusService: UpdateJobStatusService

    @Autowired
    private lateinit var jobRepository: JobRepository

    @BeforeEach
    fun setUp() {
        jobRepository.deleteAll()
    }

    @Test
    fun `job 생성 후 DB에 PENDING 상태로 저장된다`() {
        val imageUrl = "https://example.com/image.jpg"

        val response = createJobService.execute(CreateJobRequest(imageUrl))

        val saved = jobRepository.findById(response.id).get()
        assertThat(saved.imageUrl).isEqualTo(imageUrl)
        assertThat(saved.status).isEqualTo(JobStatus.PENDING)
    }

    @Test
    fun `동일한 imageUrl로 요청하면 기존 job을 반환한다`() {
        val imageUrl = "https://example.com/image.jpg"

        val first = createJobService.execute(CreateJobRequest(imageUrl))
        val second = createJobService.execute(CreateJobRequest(imageUrl))

        assertThat(first.id).isEqualTo(second.id)
        assertThat(jobRepository.count()).isEqualTo(1)
    }

    @Test
    fun `FAILED된 job과 동일한 imageUrl로 요청하면 새 job을 생성한다`() {
        val imageUrl = "https://example.com/image.jpg"
        val failedJob = jobRepository.save(Job(imageUrl = imageUrl, status = JobStatus.FAILED))

        val response = createJobService.execute(CreateJobRequest(imageUrl))

        assertThat(response.id).isNotEqualTo(failedJob.id)
        assertThat(jobRepository.count()).isEqualTo(2)
    }

    @Test
    fun `PENDING에서 PROCESSING으로 상태 전이된다`() {
        val job = jobRepository.save(Job(imageUrl = "https://example.com/image.jpg"))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(
                jobId = job.id,
                nextStatus = JobStatus.PROCESSING,
                workerJobId = "worker-001",
            )
        )

        val updated = jobRepository.findById(job.id).get()
        assertThat(updated.status).isEqualTo(JobStatus.PROCESSING)
        assertThat(updated.workerJobId).isEqualTo("worker-001")
    }

    @Test
    fun `PROCESSING에서 COMPLETED로 상태 전이되고 result가 저장된다`() {
        val job = jobRepository.save(Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.PROCESSING))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(
                jobId = job.id,
                nextStatus = JobStatus.COMPLETED,
                result = "처리 결과",
            )
        )

        val updated = jobRepository.findById(job.id).get()
        assertThat(updated.status).isEqualTo(JobStatus.COMPLETED)
        assertThat(updated.result).isEqualTo("처리 결과")
    }

    @Test
    fun `PROCESSING에서 FAILED로 상태 전이되고 failureReason이 저장된다`() {
        val job = jobRepository.save(Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.PROCESSING))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(
                jobId = job.id,
                nextStatus = JobStatus.FAILED,
                failureReason = "워커 처리 실패",
            )
        )

        val updated = jobRepository.findById(job.id).get()
        assertThat(updated.status).isEqualTo(JobStatus.FAILED)
        assertThat(updated.failureReason).isEqualTo("워커 처리 실패")
    }

    @Test
    fun `terminal 상태에서 전이 시도하면 무시된다`() {
        val job = jobRepository.save(Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.COMPLETED))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(
                jobId = job.id,
                nextStatus = JobStatus.FAILED,
            )
        )

        val unchanged = jobRepository.findById(job.id).get()
        assertThat(unchanged.status).isEqualTo(JobStatus.COMPLETED)
    }

    @Test
    fun `존재하지 않는 job 조회 시 JobNotFoundException이 발생한다`() {
        assertThatThrownBy { getJobService.execute(java.util.UUID.randomUUID()) }
            .isInstanceOf(JobNotFoundException::class.java)
    }

    @Test
    fun `job 생성 후 getJobService로 조회할 수 있다`() {
        val imageUrl = "https://example.com/image.jpg"
        val created = createJobService.execute(CreateJobRequest(imageUrl))

        val found = getJobService.execute(created.id)

        assertThat(found.id).isEqualTo(created.id)
        assertThat(found.imageUrl).isEqualTo(imageUrl)
        assertThat(found.status).isEqualTo(JobStatus.PENDING)
    }
}