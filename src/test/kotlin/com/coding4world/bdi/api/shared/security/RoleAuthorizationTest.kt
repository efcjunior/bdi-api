package com.coding4world.bdi.api.shared.security

import com.coding4world.bdi.api.bdi.application.BdiRefreshJobs
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import com.coding4world.bdi.api.bdi.infrastructure.web.BdiRefreshJobController
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.shared.ratelimit.RateLimitBucketRegistry
import com.coding4world.bdi.api.shared.ratelimit.RateLimitIdentityResolver
import com.coding4world.bdi.api.shared.ratelimit.RateLimitPolicyResolver
import com.coding4world.bdi.api.shared.ratelimit.RateLimitingFilter
import com.coding4world.bdi.api.shared.web.ApiProblemFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockServletContext
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import tools.jackson.databind.ObjectMapper
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
            .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test
    fun `wrong issuer and audience are unauthorized`() {
        mockMvc.perform(get("/api/v1/bdi/current").header("Authorization", "Bearer wrong-issuer-token"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/v1/bdi/current").header("Authorization", "Bearer wrong-audience-token"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `user role cannot access administrative endpoint`() {
        mockMvc.perform(
            get("/api/v1/admin/bdi/refresh/job-1")
                .header("Authorization", "Bearer user-token"),
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
    }

    @Test
    fun `administrator role can access administrative endpoint`() {
        mockMvc.perform(
            get("/api/v1/admin/bdi/refresh/job-1")
                .header("Authorization", "Bearer admin-token"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("job-1"))
    }

    @Test
    fun `administrator can start an asynchronous refresh`() {
        mockMvc.perform(
            post("/api/v1/admin/bdi/refresh")
                .header("Authorization", "Bearer admin-token"),
        ).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.jobId").value("job-created"))
            .andExpect(
                header().string("Location", "/api/v1/admin/bdi/refresh/job-created"),
            )
    }

    @Test
    fun `removed auth and user management endpoints are absent`() {
        mockMvc.perform(post("/api/v1/auth/login"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNotFound)

        mockMvc.perform(patch("/api/v1/admin/users/user-1").header("Authorization", "Bearer admin-token"))
            .andExpect(status().isNotFound)
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfiguration::class)
    class SecurityTestConfiguration {
        @Bean
        fun testProperties() = BdiApiProperties()

        @Bean
        fun testObjectMapper() = ObjectMapper()

        @Bean
        fun testProblemFactory() = ApiProblemFactory()

        @Bean
        fun testSecurityProblemWriter(
            objectMapper: ObjectMapper,
            problems: ApiProblemFactory,
        ) = SecurityProblemWriter(objectMapper, problems)

        @Bean
        fun testRateLimitingFilter(
            properties: BdiApiProperties,
            objectMapper: ObjectMapper,
            problems: ApiProblemFactory,
        ) = RateLimitingFilter(
            properties = properties,
            policyResolver = RateLimitPolicyResolver(),
            identityResolver = RateLimitIdentityResolver(),
            bucketRegistry = RateLimitBucketRegistry(),
            objectMapper = objectMapper,
            problems = problems,
        )

        @Bean
        fun testJwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                when (token) {
                    "wrong-issuer-token" -> throw BadJwtException("The token issuer is invalid")
                    "wrong-audience-token" -> throw BadJwtException("The required audience is missing")
                    else -> testJwt(token)
                }
            }

        private fun testJwt(token: String): Jwt {
            val role = if (token == "admin-token") "ADMIN" else "USER"
            return Jwt
                .withTokenValue(token)
                .header("alg", "RS256")
                .issuer("https://auth.coding4world.com")
                .audience(listOf("bdi-api"))
                .subject("user-1")
                .issuedAt(Instant.parse("2026-07-10T12:00:00Z"))
                .expiresAt(Instant.parse("2099-07-10T12:15:00Z"))
                .claim("roles", listOf(role))
                .build()
        }

        @Bean
        fun testRefreshJobs(): BdiRefreshJobs =
            object : BdiRefreshJobs {
                override fun request(
                    trigger: BdiRefreshTrigger,
                    requestedBy: String?,
                ): BdiRefreshJob =
                    BdiRefreshJob(
                        id = "job-created",
                        status = BdiRefreshJobStatus.PENDING,
                        trigger = trigger,
                        requestedBy = requestedBy,
                        expiresAt = Instant.parse("2026-07-17T12:00:00Z"),
                    )

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
