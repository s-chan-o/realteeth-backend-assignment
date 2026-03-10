package com.seungchan.realteeth.domain.job.scheduler

import com.seungchan.realteeth.domain.job.client.MockWorkerClient
import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.service.FindActiveJobsService
import com.seungchan.realteeth.domain.job.service.UpdateJobStatusService
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import com.seungchan.realteeth.global.config.JobProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class JobPollingSchedulerTest {

    @Mock
    private lateinit var findActiveJobsService: FindActiveJobsService

    @Mock
    private lateinit var updateJobStatusService: UpdateJobStatusService

    @Mock
    private lateinit var mockWorkerClient: MockWorkerClient

    @Mock
    private lateinit var jobProperties: JobProperties

    @InjectMocks
    private lateinit var jobPollingScheduler: JobPollingScheduler

    @Test
    fun `transitions to COMPLETED when worker returns COMPLETED`() {
        val job = processingJob()
        given(jobProperties.processingTimeoutMinutes).willReturn(30L)
        given(findActiveJobsService.execute(JobStatus.PROCESSING)).willReturn(listOf(job))
        given(mockWorkerClient.poll(job.workerJobId!!))
            .willReturn(MockWorkerClient.PollResult.Completed(job.workerJobId!!, "result-data"))

        jobPollingScheduler.pollProcessingJobs()

        verify(updateJobStatusService).execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.COMPLETED, result = "result-data")
        )
    }

    @Test
    fun `transitions to FAILED when worker returns FAILED`() {
        val job = processingJob()
        given(jobProperties.processingTimeoutMinutes).willReturn(30L)
        given(findActiveJobsService.execute(JobStatus.PROCESSING)).willReturn(listOf(job))
        given(mockWorkerClient.poll(job.workerJobId!!))
            .willReturn(MockWorkerClient.PollResult.Failed(job.workerJobId!!))

        jobPollingScheduler.pollProcessingJobs()

        verify(updateJobStatusService).execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.FAILED, failureReason = "Worker reported FAILED")
        )
    }

    @Test
    fun `does not update status when worker returns PROCESSING`() {
        val job = processingJob()
        given(jobProperties.processingTimeoutMinutes).willReturn(30L)
        given(findActiveJobsService.execute(JobStatus.PROCESSING)).willReturn(listOf(job))
        given(mockWorkerClient.poll(job.workerJobId!!))
            .willReturn(MockWorkerClient.PollResult.Processing(job.workerJobId!!))

        jobPollingScheduler.pollProcessingJobs()

        verify(updateJobStatusService, never()).execute(any())
    }

    @Test
    fun `marks as FAILED when processing timeout is exceeded`() {
        val timedOutJob = processingJob(updatedAt = LocalDateTime.now().minusMinutes(31))
        given(jobProperties.processingTimeoutMinutes).willReturn(30L)
        given(findActiveJobsService.execute(JobStatus.PROCESSING)).willReturn(listOf(timedOutJob))

        jobPollingScheduler.pollProcessingJobs()

        verify(updateJobStatusService).execute(
            UpdateJobStatusCommand(
                jobId = timedOutJob.id,
                nextStatus = JobStatus.FAILED,
                failureReason = "Processing timeout exceeded (30m)",
            )
        )
        verify(mockWorkerClient, never()).poll(any())
    }

    @Test
    fun `marks as FAILED when workerJobId is missing`() {
        val job = Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.PROCESSING)
        given(jobProperties.processingTimeoutMinutes).willReturn(30L)
        given(findActiveJobsService.execute(JobStatus.PROCESSING)).willReturn(listOf(job))

        jobPollingScheduler.pollProcessingJobs()

        verify(updateJobStatusService).execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.FAILED, failureReason = "Missing workerJobId")
        )
    }

    private fun processingJob(updatedAt: LocalDateTime = LocalDateTime.now()) = Job(
        imageUrl = "https://example.com/image.jpg",
        status = JobStatus.PROCESSING,
        workerJobId = "worker-123",
    ).also {
        it.updatedAt = updatedAt
    }
}