package com.seungchan.realteeth.domain.job.presentation

import com.seungchan.realteeth.domain.job.presentation.data.request.CreateJobRequest
import com.seungchan.realteeth.domain.job.presentation.data.response.CreateJobResponse
import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobListResponse
import com.seungchan.realteeth.domain.job.presentation.data.response.GetJobResponse
import com.seungchan.realteeth.domain.job.service.CreateJobService
import com.seungchan.realteeth.domain.job.service.GetJobListService
import com.seungchan.realteeth.domain.job.service.GetJobService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/jobs")
class JobController(
    private val createJobService: CreateJobService,
    private val getJobService: GetJobService,
    private val getJobListService: GetJobListService,
) {
    @PostMapping
    fun createJob(@Valid @RequestBody request: CreateJobRequest): ResponseEntity<CreateJobResponse> {
        val response = createJobService.execute(request)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response)
    }

    @GetMapping("/{id}")
    fun getJob(@PathVariable id: UUID): ResponseEntity<GetJobResponse> {
        val response = getJobService.execute(id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getJobList(): ResponseEntity<GetJobListResponse> {
        val response = getJobListService.execute()
        return ResponseEntity.ok(response)
    }
}