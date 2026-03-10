package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobResponse
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.GetJobService
import com.seungchan.realteeth.global.error.exception.JobNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GetJobServiceImpl(
    private val jobRepository: JobRepository,
) : GetJobService {

    @Transactional(readOnly = true)
    override fun execute(id: UUID): GetJobResponse {
        val job = jobRepository.findById(id).orElseThrow { JobNotFoundException() }
        return GetJobResponse(
            id = job.id,
            imageUrl = job.imageUrl,
            status = job.status,
            result = job.result,
            failureReason = job.failureReason,
            createdAt = job.createdAt,
            updatedAt = job.updatedAt,
        )
    }
}
