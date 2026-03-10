package com.seungchan.realteeth.domain.job.scheduler

import com.seungchan.realteeth.domain.job.client.MockWorkerClient
import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.service.FindActiveJobsService
import com.seungchan.realteeth.domain.job.service.UpdateJobStatusService
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class JobSubmitterTest {

    @Mock
    private lateinit var findActiveJobsService: FindActiveJobsService

    @Mock
    private lateinit var updateJobStatusService: UpdateJobStatusService

    @Mock
    private lateinit var mockWorkerClient: MockWorkerClient

    @InjectMocks
    private lateinit var jobSubmitter: JobSubmitter

    @Test
    fun `submits pending job and transitions to PROCESSING on success`() {
        val job = Job(imageUrl = "https://example.com/image.jpg")
        given(findActiveJobsService.execute(JobStatus.PENDING)).willReturn(listOf(job))
        given(mockWorkerClient.submit(job.imageUrl)).willReturn(MockWorkerClient.SubmitResult.Success("worker-123"))

        jobSubmitter.submitPendingJobs()

        verify(updateJobStatusService).execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.PROCESSING, workerJobId = "worker-123")
        )
    }

    @Test
    fun `leaves job as PENDING when rate limited`() {
        val job = Job(imageUrl = "https://example.com/image.jpg")
        given(findActiveJobsService.execute(JobStatus.PENDING)).willReturn(listOf(job))
        given(mockWorkerClient.submit(job.imageUrl)).willReturn(MockWorkerClient.SubmitResult.RateLimited)

        jobSubmitter.submitPendingJobs()

        verify(updateJobStatusService, never()).execute(any())
    }

    @Test
    fun `transitions to FAILED when submission fails`() {
        val job = Job(imageUrl = "https://example.com/image.jpg")
        given(findActiveJobsService.execute(JobStatus.PENDING)).willReturn(listOf(job))
        given(mockWorkerClient.submit(job.imageUrl)).willReturn(MockWorkerClient.SubmitResult.Failure("Server error"))

        jobSubmitter.submitPendingJobs()

        verify(updateJobStatusService).execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.FAILED, failureReason = "Server error")
        )
    }

    @Test
    fun `does nothing when no pending jobs`() {
        given(findActiveJobsService.execute(JobStatus.PENDING)).willReturn(emptyList())

        jobSubmitter.submitPendingJobs()

        verify(mockWorkerClient, never()).submit(any())
        verify(updateJobStatusService, never()).execute(any())
    }
}