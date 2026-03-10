package com.seungchan.realteeth.domain.job.presentation.data.request

import jakarta.validation.constraints.NotBlank

data class CreateJobRequest(
    @field:NotBlank
    val imageUrl: String,
)