package com.seungchan.realteeth.domain.job.scheduler

import com.seungchan.realteeth.domain.job.client.MockWorkerClient
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.service.FindActiveJobsService
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import com.seungchan.realteeth.domain.job.service.UpdateJobStatusService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JobSubmitter(
    private val findActiveJobsService: FindActiveJobsService,
    private val updateJobStatusService: UpdateJobStatusService,
    private val mockWorkerClient: MockWorkerClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5000)
    fun submitPendingJobs() {
        val pendingJobs = findActiveJobsService.execute(JobStatus.PENDING)
        if (pendingJobs.isEmpty()) return

        log.info("Submitting {} pending job(s) to Mock Worker", pendingJobs.size)
        pendingJobs.forEach { job ->
            when (val result = mockWorkerClient.submit(job.imageUrl)) {
                is MockWorkerClient.SubmitResult.Success -> {
                    log.info("Job {} submitted; workerJobId={}", job.id, result.jobId)
                    updateJobStatusService.execute(
                        UpdateJobStatusCommand(
                            jobId = job.id,
                            nextStatus = JobStatus.PROCESSING,
                            workerJobId = result.jobId,
                        )
                    )
                }
                is MockWorkerClient.SubmitResult.RateLimited ->
                    log.warn("Rate limited; job {} will be retried on next cycle", job.id)

                is MockWorkerClient.SubmitResult.Failure -> {
                    log.error("Job {} submission failed: {}", job.id, result.reason)
                    updateJobStatusService.execute(
                        UpdateJobStatusCommand(
                            jobId = job.id,
                            nextStatus = JobStatus.FAILED,
                            failureReason = result.reason,
                        )
                    )
                }
            }
        }
    }
}