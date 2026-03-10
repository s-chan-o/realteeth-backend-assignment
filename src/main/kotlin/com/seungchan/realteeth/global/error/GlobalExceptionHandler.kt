package com.seungchan.realteeth.global.error

import com.seungchan.realteeth.global.error.exception.RealteethException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RealteethException::class)
    fun handleRealteethException(e: RealteethException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(e.errorCode.status)
            .body(ErrorResponse(message = e.errorCode.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(400)
            .body(ErrorResponse(message = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Validation error"))
}