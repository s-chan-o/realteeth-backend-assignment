package com.seungchan.realteeth.domain.job.service

import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand

interface UpdateJobStatusService {
    fun execute(command: UpdateJobStatusCommand)
}