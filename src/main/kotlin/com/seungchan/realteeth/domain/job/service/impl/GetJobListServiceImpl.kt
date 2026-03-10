package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobListResponse
import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobResponse
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.GetJobListService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetJobListServiceImpl(
    private val jobRepository: JobRepository,
) : GetJobListService {

    @Transactional(readOnly = true)
    override fun execute(): GetJobListResponse {
        val jobs = jobRepository.findAll().map { job ->
            GetJobResponse(
                id = job.id,
                imageUrl = job.imageUrl,
                status = job.status,
                result = job.result,
                failureReason = job.failureReason,
                createdAt = job.createdAt,
                updatedAt = job.updatedAt,
            )
        }
        return GetJobListResponse(jobs = jobs, total = jobs.size)
    }
}