package com.coding4world.bdi.api.shared.ratelimit

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.shared.web.ApiProblemFactory
import com.coding4world.bdi.api.shared.web.TraceIdFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class RateLimitingFilterTest {
    @Test
    fun `auth endpoints are not rate limited by bdi api`() {
        val mockMvc = mockMvc(properties(administration = policy(capacity = 1)))

        mockMvc.perform(post("/api/v1/auth/login").from("10.0.0.1"))
            .andExpect(status().isNotFound)
            .andExpect(header().doesNotExist("RateLimit-Limit"))

        mockMvc.perform(post("/api/v1/auth/refresh").from("10.0.0.1"))
            .andExpect(status().isNotFound)
            .andExpect(header().doesNotExist("RateLimit-Limit"))
    }

    @Test
    fun `current BDI is limited per authenticated user`() {
        val mockMvc = mockMvc(properties(currentBdi = policy(capacity = 1)))

        mockMvc.perform(get("/api/v1/bdi/current").asUser("user-1"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/v1/bdi/current").asUser("user-1"))
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))

        mockMvc.perform(get("/api/v1/bdi/current").asUser("user-2"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `BDI history is limited per authenticated user`() {
        val mockMvc = mockMvc(properties(bdiHistory = policy(capacity = 1)))

        mockMvc.perform(get("/api/v1/bdi/history").asUser("user-1"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/v1/bdi/history").asUser("user-1"))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string("RateLimit-Limit", "1"))
    }

    @Test
    fun `administrative BDI endpoints share one administrator bucket`() {
        val mockMvc = mockMvc(properties(administration = policy(capacity = 1)))

        mockMvc.perform(post("/api/v1/admin/bdi/refresh").asUser("admin-1", "ADMIN"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/v1/admin/bdi/refresh/job-1").asUser("admin-1", "ADMIN"))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string("RateLimit-Limit", "1"))

        mockMvc.perform(post("/api/v1/admin/bdi/refresh").asUser("admin-2", "ADMIN"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `forwarded IP headers do not affect authenticated user buckets`() {
        val mockMvc = mockMvc(properties(currentBdi = policy(capacity = 1), trustForwardedHeaders = true))

        mockMvc.perform(
            get("/api/v1/bdi/current")
                .asUser("user-1")
                .from("10.0.0.1")
                .header("X-Forwarded-For", "203.0.113.10"),
        ).andExpect(status().isNotFound)

        mockMvc.perform(
            get("/api/v1/bdi/current")
                .asUser("user-1")
                .from("10.0.0.2")
                .header("X-Forwarded-For", "203.0.113.11"),
        ).andExpect(status().isTooManyRequests)
    }

    private fun mockMvc(properties: BdiApiProperties): MockMvc {
        val rateLimitingFilter =
            RateLimitingFilter(
                properties = properties,
                policyResolver = RateLimitPolicyResolver(),
                identityResolver = RateLimitIdentityResolver(),
                bucketRegistry = RateLimitBucketRegistry(),
                objectMapper = ObjectMapper(),
                problems = ApiProblemFactory(),
            )
        return MockMvcBuilders
            .standaloneSetup(Any())
            .addFilters<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(
                TraceIdFilter(),
                TestAuthenticationFilter(),
                rateLimitingFilter,
            )
            .build()
    }

    private fun properties(
        currentBdi: BdiApiProperties.RateLimit.Policy = policy(capacity = 100),
        bdiHistory: BdiApiProperties.RateLimit.Policy = policy(capacity = 100),
        administration: BdiApiProperties.RateLimit.Policy = policy(capacity = 100),
        trustForwardedHeaders: Boolean = false,
    ): BdiApiProperties =
        BdiApiProperties(
            rateLimit =
                BdiApiProperties.RateLimit(
                    trustForwardedHeaders = trustForwardedHeaders,
                    currentBdi = currentBdi,
                    bdiHistory = bdiHistory,
                    administration = administration,
                ),
        )

    private companion object {
        fun policy(capacity: Long): BdiApiProperties.RateLimit.Policy =
            BdiApiProperties.RateLimit.Policy(capacity = capacity, refillPeriod = Duration.ofMinutes(1))
    }
}

private class TestAuthenticationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val subject = request.getHeader(SUBJECT_HEADER)
        if (subject == null) {
            filterChain.doFilter(request, response)
            return
        }

        val authorities =
            request
                .getHeader(ROLES_HEADER)
                .orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { SimpleGrantedAuthority("ROLE_$it") }
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(subject, "n/a", authorities)
        try {
            filterChain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }

    companion object {
        const val SUBJECT_HEADER = "X-Test-Subject"
        const val ROLES_HEADER = "X-Test-Roles"
    }
}

private fun MockHttpServletRequestBuilder.from(remoteAddr: String): MockHttpServletRequestBuilder =
    with(
        RequestPostProcessor { request ->
            request.remoteAddr = remoteAddr
            request
        },
    )

private fun MockHttpServletRequestBuilder.asUser(
    subject: String,
    vararg roles: String = arrayOf("USER"),
): MockHttpServletRequestBuilder =
    header(TestAuthenticationFilter.SUBJECT_HEADER, subject)
        .header(TestAuthenticationFilter.ROLES_HEADER, roles.joinToString(","))
        .accept(MediaType.APPLICATION_JSON)
