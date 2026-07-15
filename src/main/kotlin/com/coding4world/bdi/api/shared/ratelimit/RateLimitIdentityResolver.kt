package com.coding4world.bdi.api.shared.ratelimit

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class RateLimitIdentityResolver {
    fun resolve(
        policy: RateLimitPolicy,
        request: HttpServletRequest,
        properties: BdiApiProperties.RateLimit,
    ): String? =
        when (policy) {
            RateLimitPolicy.CURRENT_BDI,
            RateLimitPolicy.BDI_HISTORY,
            -> authenticatedUser()?.let { "user:$it" }

            RateLimitPolicy.ADMINISTRATION ->
                if (isAdministrator()) {
                    authenticatedUser()?.let { "admin:$it" }
                } else {
                    null
                }
        }

    private fun authenticatedUser(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated) return null
        return when (val principal = authentication.principal) {
            is Jwt -> principal.subject
            is String -> principal
            else -> authentication.name
        }?.takeIf(String::isNotBlank)
    }

    private fun isAdministrator(): Boolean =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.authorities
            ?.any { it.authority == "ROLE_ADMIN" }
            ?: false
}
