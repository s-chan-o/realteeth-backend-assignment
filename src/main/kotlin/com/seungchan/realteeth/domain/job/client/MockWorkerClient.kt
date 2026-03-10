package com.seungchan.realteeth.domain.job.client

import com.seungchan.realteeth.domain.job.client.constant.MockJobStatus
import com.seungchan.realteeth.domain.job.client.dto.request.MockProcessRequest
import com.seungchan.realteeth.domain.job.client.dto.response.MockProcessStartResponse
import com.seungchan.realteeth.domain.job.client.dto.response.MockProcessStatusResponse
import com.seungchan.realteeth.global.config.MockWorkerProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class MockWorkerClient(
    private val mockWorkerRestClient: RestClient,
    private val properties: MockWorkerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    sealed interface SubmitResult {
        data class Success(val jobId: String) : SubmitResult
        data object RateLimited : SubmitResult
        data class Failure(val reason: String) : SubmitResult
    }

    sealed interface PollResult {
        data class Processing(val jobId: String) : PollResult
        data class Completed(val jobId: String, val result: String?) : PollResult
        data class Failed(val jobId: String) : PollResult
        data class Error(val reason: String) : PollResult
    }

    fun submit(imageUrl: String): SubmitResult =
        try {
            val response = mockWorkerRestClient.post()
                .uri("/process")
                .header("X-API-KEY", properties.apiKey)
                .body(MockProcessRequest(imageUrl = imageUrl))
                .retrieve()
                .body(MockProcessStartResponse::class.java)!!
            SubmitResult.Success(response.jobId)
        } catch (e: RestClientResponseException) {
            when (e.statusCode) {
                HttpStatus.TOO_MANY_REQUESTS -> {
                    log.warn("Mock Worker rate limited; will retry on next cycle")
                    SubmitResult.RateLimited
                }
                else -> {
                    log.error("Mock Worker submission failed [{}]: {}", e.statusCode, e.responseBodyAsString)
                    SubmitResult.Failure(e.responseBodyAsString)
                }
            }
        } catch (e: Exception) {
            log.error("Mock Worker submission error", e)
            SubmitResult.Failure(e.message ?: "Unknown error")
        }

    fun poll(workerJobId: String): PollResult =
        try {
            val response = mockWorkerRestClient.get()
                .uri("/process/{jobId}", workerJobId)
                .retrieve()
                .body(MockProcessStatusResponse::class.java)!!
            when (response.status) {
                MockJobStatus.COMPLETED -> PollResult.Completed(response.jobId, response.result)
                MockJobStatus.FAILED -> PollResult.Failed(response.jobId)
                MockJobStatus.PROCESSING -> PollResult.Processing(response.jobId)
            }
        } catch (e: RestClientResponseException) {
            log.error("Mock Worker poll failed [{}]: {}", e.statusCode, e.responseBodyAsString)
            PollResult.Error(e.responseBodyAsString)
        } catch (e: Exception) {
            log.error("Mock Worker poll error", e)
            PollResult.Error(e.message ?: "Unknown error")
        }
}