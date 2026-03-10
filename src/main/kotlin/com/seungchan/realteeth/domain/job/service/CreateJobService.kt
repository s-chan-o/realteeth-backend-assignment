package com.seungchan.realteeth.domain.job.service

import com.seungchan.realteeth.domain.job.presentation.data.request.CreateJobRequest
import com.seungchan.realteeth.domain.job.presentation.data.response.CreateJobResponse

interface CreateJobService {
    fun execute(request: CreateJobRequest): CreateJobResponse
}