package com.coding4world.bdi.api.shared.ratelimit

import com.coding4world.bdi.api.shared.config.BdiApiProperties

enum class RateLimitPolicy {
    LOGIN,
    TOKEN_REFRESH,
    CURRENT_BDI,
    BDI_HISTORY,
    ADMINISTRATION,
}

fun BdiApiProperties.RateLimit.policy(policy: RateLimitPolicy): BdiApiProperties.RateLimit.Policy =
    when (policy) {
        RateLimitPolicy.LOGIN -> login
        RateLimitPolicy.TOKEN_REFRESH -> refresh
        RateLimitPolicy.CURRENT_BDI -> currentBdi
        RateLimitPolicy.BDI_HISTORY -> bdiHistory
        RateLimitPolicy.ADMINISTRATION -> administration
    }
