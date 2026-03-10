package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.domain.job.service.command.UpdateJobStatusCommand
import com.seungchan.realteeth.global.error.exception.JobNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UpdateJobStatusServiceImplTest {

    @Mock
    private lateinit var jobRepository: JobRepository

    @InjectMocks
    private lateinit var updateJobStatusService: UpdateJobStatusServiceImpl

    @Test
    fun `transitions PENDING to PROCESSING and sets workerJobId`() {
        val job = Job(imageUrl = "https://example.com/image.jpg")
        given(jobRepository.findByIdWithLock(job.id)).willReturn(Optional.of(job))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(
                jobId = job.id,
                nextStatus = JobStatus.PROCESSING,
                workerJobId = "worker-123",
            )
        )

        assertThat(job.status).isEqualTo(JobStatus.PROCESSING)
        assertThat(job.workerJobId).isEqualTo("worker-123")
    }

    @Test
    fun `skips update when job is already in terminal state`() {
        val job = Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.COMPLETED)
        given(jobRepository.findByIdWithLock(job.id)).willReturn(Optional.of(job))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.FAILED)
        )

        assertThat(job.status).isEqualTo(JobStatus.COMPLETED)
    }

    @Test
    fun `sets default failureReason when FAILED without reason`() {
        val job = Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.PROCESSING)
        given(jobRepository.findByIdWithLock(job.id)).willReturn(Optional.of(job))

        updateJobStatusService.execute(
            UpdateJobStatusCommand(jobId = job.id, nextStatus = JobStatus.FAILED)
        )

        assertThat(job.status).isEqualTo(JobStatus.FAILED)
        assertThat(job.failureReason).isEqualTo("Unknown failure")
    }

    @Test
    fun `throws JobNotFoundException when job does not exist`() {
        val id = UUID.randomUUID()
        given(jobRepository.findByIdWithLock(id)).willReturn(Optional.empty())

        assertThatThrownBy {
            updateJobStatusService.execute(UpdateJobStatusCommand(jobId = id, nextStatus = JobStatus.PROCESSING))
        }.isInstanceOf(JobNotFoundException::class.java)
    }
}