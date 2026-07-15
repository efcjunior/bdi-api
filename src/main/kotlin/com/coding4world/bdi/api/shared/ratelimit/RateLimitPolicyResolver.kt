package com.coding4world.bdi.api.shared.ratelimit

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class RateLimitPolicyResolver {
    fun resolve(request: HttpServletRequest): RateLimitPolicy? {
        val path = request.pathWithinApplication()
        return when {
            request.method == "GET" && path == "/api/v1/bdi/current" -> RateLimitPolicy.CURRENT_BDI
            request.method == "GET" && path == "/api/v1/bdi/history" -> RateLimitPolicy.BDI_HISTORY
            path.startsWith("/api/v1/admin/") -> RateLimitPolicy.ADMINISTRATION
            else -> null
        }
    }

    private fun HttpServletRequest.pathWithinApplication(): String {
        val context = contextPath.orEmpty()
        return if (context.isNotBlank() && requestURI.startsWith(context)) {
            requestURI.removePrefix(context)
        } else {
            requestURI
        }
    }
}
