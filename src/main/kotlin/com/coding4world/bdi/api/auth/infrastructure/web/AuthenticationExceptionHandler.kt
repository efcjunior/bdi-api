package com.coding4world.bdi.api.auth.infrastructure.web

import com.coding4world.bdi.api.auth.application.InvalidAuthenticationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthenticationExceptionHandler {
    @ExceptionHandler(InvalidAuthenticationException::class)
    fun invalidAuthentication(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "The supplied authentication credentials are invalid").apply {
            title = "Authentication failed"
            setProperty("code", "INVALID_AUTHENTICATION")
        }
}
