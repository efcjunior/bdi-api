package com.coding4world.bdi.api.auth.infrastructure.security

import com.coding4world.bdi.api.bdi.application.BdiRefreshJobs
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import com.coding4world.bdi.api.bdi.infrastructure.web.BdiRefreshJobController
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

class RoleAuthorizationTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var context: AnnotationConfigWebApplicationContext

    @BeforeEach
    fun setUp() {
        context = AnnotationConfigWebApplicationContext()
        context.servletContext = MockServletContext()
        context.register(SecurityTestConfiguration::class.java)
        context.refresh()
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @AfterEach
    fun tearDown() {
        context.close()
    }

    @Test
    fun `missing access token is unauthorized`() {
        mockMvc.perform(get("/api/v1/admin/bdi/refresh/job-1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `user role cannot access administrative endpoint`() {
        mockMvc.perform(
            get("/api/v1/admin/bdi/refresh/job-1")
                .header("Authorization", "Bearer user-token"),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `administrator role can access administrative endpoint`() {
        mockMvc.perform(
            get("/api/v1/admin/bdi/refresh/job-1")
                .header("Authorization", "Bearer admin-token"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("job-1"))
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfiguration::class)
    class SecurityTestConfiguration {
        @Bean
        fun testJwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                val role = if (token == "admin-token") "ADMIN" else "USER"
                Jwt
                    .withTokenValue(token)
                    .header("alg", "RS256")
                    .subject("user-1")
                    .issuedAt(Instant.parse("2026-07-10T12:00:00Z"))
                    .expiresAt(Instant.parse("2026-07-10T12:15:00Z"))
                    .claim("roles", listOf(role))
                    .build()
            }

        @Bean
        fun testRefreshJobs(): BdiRefreshJobs =
            object : BdiRefreshJobs {
                override fun request(
                    trigger: BdiRefreshTrigger,
                    requestedBy: String?,
                ): BdiRefreshJob = error("Refresh requests are not used by this test")

                override fun findById(jobId: String): BdiRefreshJob =
                    BdiRefreshJob(
                        id = jobId,
                        status = BdiRefreshJobStatus.SUCCEEDED,
                        trigger = BdiRefreshTrigger.ADMIN,
                        expiresAt = Instant.parse("2026-07-17T12:00:00Z"),
                    )

                override fun recoverOrphanedJobs(): Int = 0
            }

        @Bean
        fun bdiRefreshJobController(refreshJobs: BdiRefreshJobs) = BdiRefreshJobController(refreshJobs)
    }
}
