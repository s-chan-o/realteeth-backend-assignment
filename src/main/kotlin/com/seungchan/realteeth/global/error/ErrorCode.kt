package com.seungchan.realteeth.global.error

enum class ErrorCode(
    val status: Int,
    val message: String,
) {
    JOB_NOT_FOUND(404, "Job not found"),
    INVALID_JOB_STATE_TRANSITION(409, "Invalid job state transition"),
    INVALID_IMAGE_URL(400, "Invalid image URL format"),
    PROCESSING_TIMEOUT(408, "Job processing timed out"),
}
