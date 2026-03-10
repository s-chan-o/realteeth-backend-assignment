package com.seungchan.realteeth.domain.job.service.impl

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.domain.job.repository.JobRepository
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
class GetJobServiceImplTest {

    @Mock
    private lateinit var jobRepository: JobRepository

    @InjectMocks
    private lateinit var getJobService: GetJobServiceImpl

    @Test
    fun `returns job response when job exists`() {
        val job = Job(imageUrl = "https://example.com/image.jpg", status = JobStatus.COMPLETED, result = "result-data")
        given(jobRepository.findById(job.id)).willReturn(Optional.of(job))

        val response = getJobService.execute(job.id)

        assertThat(response.id).isEqualTo(job.id)
        assertThat(response.status).isEqualTo(JobStatus.COMPLETED)
        assertThat(response.result).isEqualTo("result-data")
    }

    @Test
    fun `throws JobNotFoundException when job does not exist`() {
        val id = UUID.randomUUID()
        given(jobRepository.findById(id)).willReturn(Optional.empty())

        assertThatThrownBy { getJobService.execute(id) }
            .isInstanceOf(JobNotFoundException::class.java)
    }
}