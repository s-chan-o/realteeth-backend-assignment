package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import com.seungchan.realteeth.domain.job.service.UpdateJobStatusService
import com.seungchan.realteeth.global.error.exception.JobNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateJobStatusServiceImpl(
    private val jobRepository: JobRepository,
) : UpdateJobStatusService {

    @Transactional
    override fun execute(command: UpdateJobStatusCommand) {
        val job = jobRepository.findByIdWithLock(command.jobId)
            .orElseThrow { JobNotFoundException() }

        if (!job.status.canTransitionTo(command.nextStatus)) {
            return
        }

        job.transitionTo(command.nextStatus)

        when (command.nextStatus) {
            JobStatus.PROCESSING -> job.workerJobId = command.workerJobId
            JobStatus.COMPLETED -> job.result = command.result
            JobStatus.FAILED -> job.failureReason = command.failureReason ?: "Unknown failure"
            else -> Unit
        }
    }
}