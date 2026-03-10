package com.seungchan.realteeth.domain.job.repository

import com.seungchan.realteeth.domain.job.entity.Job
import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface JobRepository : JpaRepository<Job, UUID> {
    fun findAllByStatus(status: JobStatus): List<Job>
    fun findFirstByImageUrlAndStatusIn(imageUrl: String, statuses: List<JobStatus>): Job?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    fun findByIdWithLock(id: UUID): Optional<Job>
}