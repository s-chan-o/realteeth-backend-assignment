package com.seungchan.realteeth.domain.job.service.command

import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import java.util.UUID

data class UpdateJobStatusCommand(
    val jobId: UUID,
    val nextStatus: JobStatus,
    val workerJobId: String? = null,
    val result: String? = null,
    val failureReason: String? = null,
)