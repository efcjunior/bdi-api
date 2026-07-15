package com.coding4world.bdi.api.shared.ratelimit

import com.coding4world.bdi.api.shared.config.BdiApiProperties

enum class RateLimitPolicy {
    CURRENT_BDI,
    BDI_HISTORY,
    ADMINISTRATION,
}

fun BdiApiProperties.RateLimit.policy(policy: RateLimitPolicy): BdiApiProperties.RateLimit.Policy =
    when (policy) {
        RateLimitPolicy.CURRENT_BDI -> currentBdi
        RateLimitPolicy.BDI_HISTORY -> bdiHistory
        RateLimitPolicy.ADMINISTRATION -> administration
    }
