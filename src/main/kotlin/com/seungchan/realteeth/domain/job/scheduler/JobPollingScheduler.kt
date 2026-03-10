package com.seungchan.realteeth.domain.job.scheduler

import com.seungchan.realteeth.domain.job.client.MockWorkerClient
import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.service.FindActiveJobsService
import com.seungchan.realteeth.domain.job.service.UpdateJobStatusService
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import com.seungchan.realteeth.global.config.JobProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobPollingScheduler(
    private val findActiveJobsService: FindActiveJobsService,
    private val updateJobStatusService: UpdateJobStatusService,
    private val mockWorkerClient: MockWorkerClient,
    private val jobProperties: JobProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 10000)
    fun pollProcessingJobs() {
        val processingJobs = findActiveJobsService.execute(JobStatus.PROCESSING)
        if (processingJobs.isEmpty()) return

        log.info("Polling {} processing job(s)", processingJobs.size)
        processingJobs.forEach { job ->
            if (isTimedOut(job)) {
                log.warn("Job {} timed out after {} minutes; marking as FAILED", job.id, jobProperties.processingTimeoutMinutes)
                updateJobStatusService.execute(
                    UpdateJobStatusCommand(
                        jobId = job.id,
                        nextStatus = JobStatus.FAILED,
                        failureReason = "Processing timeout exceeded (${jobProperties.processingTimeoutMinutes}m)",
                    )
                )
                return@forEach
            }

            val workerJobId = job.workerJobId
            if (workerJobId == null) {
                log.warn("PROCESSING job {} has no workerJobId; marking as FAILED", job.id)
                updateJobStatusService.execute(
                    UpdateJobStatusCommand(
                        jobId = job.id,
                        nextStatus = JobStatus.FAILED,
                        failureReason = "Missing workerJobId",
                    )
                )
                return@forEach
            }

            when (val result = mockWorkerClient.poll(workerJobId)) {
                is MockWorkerClient.PollResult.Completed -> {
                    log.info("Job {} completed", job.id)
                    updateJobStatusService.execute(
                        UpdateJobStatusCommand(
                            jobId = job.id,
                            nextStatus = JobStatus.COMPLETED,
                            result = result.result,
                        )
                    )
                }
                is MockWorkerClient.PollResult.Failed -> {
                    log.warn("Job {} failed by worker", job.id)
                    updateJobStatusService.execute(
                        UpdateJobStatusCommand(
                            jobId = job.id,
                            nextStatus = JobStatus.FAILED,
                            failureReason = "Worker reported FAILED",
                        )
                    )
                }
                is MockWorkerClient.PollResult.Processing ->
                    log.debug("Job {} still processing", job.id)

                is MockWorkerClient.PollResult.Error ->
                    log.error("Polling error for job {}: {}", job.id, result.reason)
            }
        }
    }

    private fun isTimedOut(job: Job): Boolean =
        job.updatedAt.isBefore(LocalDateTime.now().minusMinutes(jobProperties.processingTimeoutMinutes))
}