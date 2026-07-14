package com.coding4world.bdi.api.auth.infrastructure.web

import com.coding4world.bdi.api.auth.application.AuthenticationOperations
import com.coding4world.bdi.api.auth.application.AuthenticationTokens
import com.coding4world.bdi.api.auth.application.InvalidAuthenticationException
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.shared.web.ApiExceptionHandler
import com.coding4world.bdi.api.shared.web.ApiProblemFactory
import com.coding4world.bdi.api.shared.web.TraceIdFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class AuthenticationControllerContractTest {
    private val authentication = StubAuthenticationOperations()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(AuthenticationController(authentication, BdiApiProperties()))
                .setControllerAdvice(ApiExceptionHandler(ApiProblemFactory()))
                .addFilters<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(TraceIdFilter())
                .build()
    }

    @Test
    fun `login returns bearer access and refresh tokens`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"correct-password"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
    }

    @Test
    fun `invalid email returns validation problem`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"invalid","password":"password"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    fun `invalid credentials return generic unauthorized problem`() {
        authentication.rejectLogin = true

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"wrong-password"}"""),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("INVALID_AUTHENTICATION"))
            .andExpect(jsonPath("$.detail").value("The supplied authentication credentials are invalid"))
    }

    @Test
    fun `logout returns no content`() {
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"refresh-token"}"""),
        ).andExpect(status().isNoContent)
    }
}

private class StubAuthenticationOperations : AuthenticationOperations {
    var rejectLogin = false

    override fun login(
        email: String,
        password: String,
    ): AuthenticationTokens {
        if (rejectLogin) throw InvalidAuthenticationException()
        return tokens()
    }

    override fun refresh(rawRefreshToken: String): AuthenticationTokens = tokens()

    override fun logout(rawRefreshToken: String) = Unit

    private fun tokens() =
        AuthenticationTokens(
            accessToken = "access-token",
            accessTokenExpiresAt = Instant.parse("2026-07-14T12:15:00Z"),
            refreshToken = "refresh-token",
            refreshTokenExpiresAt = Instant.parse("2026-07-21T12:00:00Z"),
        )
}
