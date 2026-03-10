package com.seungchan.realteeth.domain.job.service

import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobResponse
import java.util.UUID

interface GetJobService {
    fun execute(id: UUID): GetJobResponse
}