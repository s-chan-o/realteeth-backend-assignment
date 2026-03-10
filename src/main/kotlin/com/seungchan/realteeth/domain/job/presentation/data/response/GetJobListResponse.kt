package com.seungchan.realteeth.domain.job.presentation.data.response

data class GetJobListResponse(
    val jobs: List<GetJobResponse>,
    val total: Int,
)