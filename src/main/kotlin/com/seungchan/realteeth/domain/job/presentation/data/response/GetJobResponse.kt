package com.seungchan.realteeth.domain.job.presentation.data.response

import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import java.time.LocalDateTime
import java.util.UUID

data class GetJobResponse(
    val id: UUID,
    val imageUrl: String,
    val status: JobStatus,
    val result: String?,
    val failureReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)