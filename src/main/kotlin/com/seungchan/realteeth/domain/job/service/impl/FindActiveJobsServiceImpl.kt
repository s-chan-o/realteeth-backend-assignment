package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.FindActiveJobsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FindActiveJobsServiceImpl(
    private val jobRepository: JobRepository,
) : FindActiveJobsService {

    @Transactional(readOnly = true)
    override fun execute(status: JobStatus): List<Job> =
        jobRepository.findAllByStatus(status)
}