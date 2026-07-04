package com.coding4world.bdi.api.auth.infrastructure.web

import com.coding4world.bdi.api.auth.application.AuthenticationService
import com.coding4world.bdi.api.auth.application.AuthenticationTokens
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authenticationService: AuthenticationService,
    private val properties: BdiApiProperties,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): AuthenticationResponse =
        authenticationService
            .login(request.email, request.password)
            .toResponse(properties.security.jwt.accessTokenTtl.seconds)

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): AuthenticationResponse =
        authenticationService
            .refresh(request.refreshToken)
            .toResponse(properties.security.jwt.accessTokenTtl.seconds)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @Valid @RequestBody request: RefreshRequest,
    ) {
        authenticationService.logout(request.refreshToken)
    }
}

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String,
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String,
)

data class AuthenticationResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val expiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant,
)

private fun AuthenticationTokens.toResponse(expiresIn: Long) =
    AuthenticationResponse(
        accessToken = accessToken,
        tokenType = "Bearer",
        expiresIn = expiresIn,
        expiresAt = accessTokenExpiresAt,
        refreshToken = refreshToken,
        refreshTokenExpiresAt = refreshTokenExpiresAt,
    )
