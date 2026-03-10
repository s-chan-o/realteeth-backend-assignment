package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.presentation.data.request.CreateJobRequest
import com.seungchan.realteeth.domain.job.presentation.data.response.CreateJobResponse
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.CreateJobService
import com.seungchan.realteeth.global.error.exception.InvalidImageUrlException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.net.URI

@Service
class CreateJobServiceImpl(
    private val jobRepository: JobRepository,
) : CreateJobService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    override fun execute(request: CreateJobRequest): CreateJobResponse {
        validateImageUrl(request.imageUrl)

        val existingJob = jobRepository.findFirstByImageUrlAndStatusIn(
            imageUrl = request.imageUrl,
            statuses = listOf(JobStatus.PENDING, JobStatus.PROCESSING, JobStatus.COMPLETED),
        )
        if (existingJob != null) {
            return existingJob.toCreateJobResponse()
        }

        val job = jobRepository.save(Job(imageUrl = request.imageUrl))
        return job.toCreateJobResponse()
    }

    private fun validateImageUrl(url: String) {
        val uri = runCatching { URI(url) }.getOrElse { throw InvalidImageUrlException() }
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            throw InvalidImageUrlException()
        }
    }

    private fun Job.toCreateJobResponse() = CreateJobResponse(
        id = id,
        imageUrl = imageUrl,
        status = status,
        createdAt = createdAt,
    )
}