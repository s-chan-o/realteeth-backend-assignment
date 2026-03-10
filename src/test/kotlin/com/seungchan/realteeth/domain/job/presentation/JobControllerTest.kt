package com.seungchan.realteeth.domain.job.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.presentation.data.request.CreateJobRequest
import com.seungchan.realteeth.domain.job.presentation.data.response.CreateJobResponse
import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobListResponse
import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobResponse
import com.seungchan.realteeth.domain.job.service.CreateJobService
import com.seungchan.realteeth.domain.job.service.GetJobListService
import com.seungchan.realteeth.domain.job.service.GetJobService
import com.seungchan.realteeth.global.error.GlobalExceptionHandler
import com.seungchan.realteeth.global.error.exception.JobNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(JobController::class, GlobalExceptionHandler::class)
class JobControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var createJobService: CreateJobService

    @MockitoBean
    private lateinit var getJobService: GetJobService

    @MockitoBean
    private lateinit var getJobListService: GetJobListService

    @Test
    fun `POST api-jobs returns 202 with job response`() {
        val imageUrl = "https://example.com/image.jpg"
        val jobId = UUID.randomUUID()
        given(createJobService.execute(any())).willReturn(
            CreateJobResponse(id = jobId, imageUrl = imageUrl, status = JobStatus.PENDING, createdAt = LocalDateTime.now())
        )

        mockMvc.post("/api/jobs") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateJobRequest(imageUrl))
        }.andExpect {
            status { isAccepted() }
            jsonPath("$.id") { value(jobId.toString()) }
            jsonPath("$.status") { value("PENDING") }
        }
    }

    @Test
    fun `POST api-jobs returns 400 when imageUrl is blank`() {
        mockMvc.post("/api/jobs") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateJobRequest(""))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `GET api-jobs-id returns 200 with job response`() {
        val jobId = UUID.randomUUID()
        given(getJobService.execute(jobId)).willReturn(
            GetJobResponse(
                id = jobId,
                imageUrl = "https://example.com/image.jpg",
                status = JobStatus.COMPLETED,
                result = "result-data",
                failureReason = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        )

        mockMvc.get("/api/jobs/$jobId")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("COMPLETED") }
                jsonPath("$.result") { value("result-data") }
            }
    }

    @Test
    fun `GET api-jobs-id returns 404 when job not found`() {
        val jobId = UUID.randomUUID()
        given(getJobService.execute(jobId)).willThrow(JobNotFoundException())

        mockMvc.get("/api/jobs/$jobId")
            .andExpect {
                status { isNotFound() }
            }
    }

    @Test
    fun `GET api-jobs returns 200 with job list`() {
        given(getJobListService.execute()).willReturn(
            GetJobListResponse(jobs = emptyList(), total = 0)
        )

        mockMvc.get("/api/jobs")
            .andExpect {
                status { isOk() }
                jsonPath("$.total") { value(0) }
            }
    }
}