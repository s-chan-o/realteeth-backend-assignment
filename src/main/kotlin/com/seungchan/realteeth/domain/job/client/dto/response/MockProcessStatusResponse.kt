package com.seungchan.realteeth.domain.job.client.dto.response

import com.seungchan.realteeth.domain.job.client.constant.MockJobStatus

data class MockProcessStatusResponse(
    val jobId: String,
    val status: MockJobStatus,
    val result: String?,
)