package com.san.portfolio.upstream.interceptor

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

data class ErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String
)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UpstreamTimeoutException::class)
    fun handleTimeout(ex: UpstreamTimeoutException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(Instant.now().toString(), HttpStatus.GATEWAY_TIMEOUT.value(), "Gateway Timeout", ex.message ?: "")
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body)
    }

    @ExceptionHandler(UpstreamBadGatewayException::class)
    fun handleBadGateway(ex: UpstreamBadGatewayException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(Instant.now().toString(), HttpStatus.BAD_GATEWAY.value(), "Bad Gateway", ex.message ?: "")
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body)
    }
}
