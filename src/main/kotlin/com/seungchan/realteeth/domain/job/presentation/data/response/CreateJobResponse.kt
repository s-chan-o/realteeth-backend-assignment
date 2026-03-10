package com.seungchan.realteeth.domain.job.presentation.data.response

import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import java.time.LocalDateTime
import java.util.UUID

data class CreateJobResponse(
    val id: UUID,
    val imageUrl: String,
    val status: JobStatus,
    val createdAt: LocalDateTime,
)