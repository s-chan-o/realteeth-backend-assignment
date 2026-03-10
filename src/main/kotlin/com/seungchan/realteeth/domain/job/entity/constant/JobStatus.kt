package com.seungchan.realteeth.domain.job.entity.constant

enum class JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    fun canTransitionTo(next: JobStatus): Boolean = when (this) {
        PENDING -> next == PROCESSING || next == FAILED
        PROCESSING -> next == COMPLETED || next == FAILED
        COMPLETED -> false
        FAILED -> false
    }
}