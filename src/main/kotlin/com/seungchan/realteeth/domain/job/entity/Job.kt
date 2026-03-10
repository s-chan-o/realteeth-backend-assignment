package com.seungchan.realteeth.domain.job.entity

import com.seungchan.realteeth.domain.job.entity.constant.JobStatus
import com.seungchan.realteeth.global.error.exception.InvalidJobStateTransitionException
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "jobs",
    indexes = [
        Index(name = "idx_jobs_image_url", columnList = "imageUrl"),
        Index(name = "idx_jobs_status", columnList = "status"),
    ],
)
class Job(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 2048)
    val imageUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.PENDING,

    @Column
    var workerJobId: String? = null,

    @Column(columnDefinition = "TEXT")
    var result: String? = null,

    @Column(length = 1024)
    var failureReason: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun transitionTo(next: JobStatus) {
        if (!status.canTransitionTo(next)) {
            throw InvalidJobStateTransitionException()
        }
        status = next
        updatedAt = LocalDateTime.now()
    }
}