package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.presentation.data.request.CreateJobRequest
import com.seungchan.realteeth.domain.job.repository.JobRepository
import com.seungchan.realteeth.global.error.exception.InvalidImageUrlException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class CreateJobServiceImplTest {

    @Mock
    private lateinit var jobRepository: JobRepository

    @InjectMocks
    private lateinit var createJobService: CreateJobServiceImpl

    @Test
    fun `creates new job when no active job exists`() {
        val imageUrl = "https://example.com/image.jpg"
        val job = Job(imageUrl = imageUrl)
        given(jobRepository.findFirstByImageUrlAndStatusIn(any(), any())).willReturn(null)
        given(jobRepository.save(any<Job>())).willReturn(job)

        val response = createJobService.execute(CreateJobRequest(imageUrl))

        assertThat(response.imageUrl).isEqualTo(imageUrl)
        assertThat(response.status).isEqualTo(JobStatus.PENDING)
        verify(jobRepository).save(any())
    }

    @Test
    fun `returns existing job when active job with same imageUrl exists`() {
        val imageUrl = "https://example.com/image.jpg"
        val existingJob = Job(imageUrl = imageUrl, status = JobStatus.PROCESSING)
        given(jobRepository.findFirstByImageUrlAndStatusIn(any(), any())).willReturn(existingJob)

        val response = createJobService.execute(CreateJobRequest(imageUrl))

        assertThat(response.status).isEqualTo(JobStatus.PROCESSING)
        verify(jobRepository, never()).save(any())
    }

    @Test
    fun `creates new job when previous job is FAILED`() {
        val imageUrl = "https://example.com/image.jpg"
        val newJob = Job(imageUrl = imageUrl)
        given(jobRepository.findFirstByImageUrlAndStatusIn(any(), any())).willReturn(null)
        given(jobRepository.save(any<Job>())).willReturn(newJob)

        val response = createJobService.execute(CreateJobRequest(imageUrl))

        assertThat(response.status).isEqualTo(JobStatus.PENDING)
        verify(jobRepository).save(any())
    }

    @Test
    fun `throws InvalidImageUrlException for invalid URL`() {
        assertThatThrownBy { createJobService.execute(CreateJobRequest("not-a-url")) }
            .isInstanceOf(InvalidImageUrlException::class.java)
    }

    @Test
    fun `throws InvalidImageUrlException for non http URL`() {
        assertThatThrownBy { createJobService.execute(CreateJobRequest("ftp://example.com/image.jpg")) }
            .isInstanceOf(InvalidImageUrlException::class.java)
    }
}