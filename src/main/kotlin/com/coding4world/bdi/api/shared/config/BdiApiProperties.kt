package com.coding4world.bdi.api.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("bdi-api")
data class BdiApiProperties(
    val synchronization: Synchronization = Synchronization(),
    val security: Security = Security(),
    val openApi: OpenApi = OpenApi(),
    val rateLimit: RateLimit = RateLimit(),
) {
    data class Synchronization(
        val enabled: Boolean = true,
        val interval: Duration = Duration.ofHours(6),
        val staleAfter: Duration = Duration.ofHours(12),
        val jobRetention: Duration = Duration.ofDays(7),
    )

    data class Security(
        val jwt: Jwt = Jwt(),
    )

    data class Jwt(
        val issuer: String = "https://auth.coding4world.com",
        val audience: String = "bdi-api",
        val jwksUri: String = "",
    )

    data class OpenApi(
        val public: Boolean = false,
    )

    data class RateLimit(
        val enabled: Boolean = true,
        val trustForwardedHeaders: Boolean = false,
        val currentBdi: Policy = Policy(60, Duration.ofMinutes(1)),
        val bdiHistory: Policy = Policy(30, Duration.ofMinutes(1)),
        val administration: Policy = Policy(5, Duration.ofHours(1)),
    ) {
        data class Policy(
            val capacity: Long,
            val refillPeriod: Duration,
        )
    }
}
