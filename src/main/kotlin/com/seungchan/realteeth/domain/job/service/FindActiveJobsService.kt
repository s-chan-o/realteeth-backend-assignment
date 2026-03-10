package com.seungchan.realteeth.domain.job.service

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus

interface FindActiveJobsService {
    fun execute(status: JobStatus): List<Job>
}