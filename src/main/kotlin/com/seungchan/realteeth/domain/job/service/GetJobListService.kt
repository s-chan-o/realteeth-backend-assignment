package com.seungchan.realteeth.domain.job.service

import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobListResponse

interface GetJobListService {
    fun execute(): GetJobListResponse
}