package com.coding4world.bdi.api.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("bdi-api")
data class BdiApiProperties(
    val synchronization: Synchronization = Synchronization(),
    val security: Security = Security(),
    val openApi: OpenApi = OpenApi(),
) {
    data class Synchronization(
        val enabled: Boolean = true,
        val interval: Duration = Duration.ofHours(6),
        val staleAfter: Duration = Duration.ofHours(12),
        val jobRetention: Duration = Duration.ofDays(7),
    )

    data class Security(
        val jwt: Jwt = Jwt(),
        val refreshTokenTtl: Duration = Duration.ofDays(7),
        val bootstrapAdmin: BootstrapAdmin = BootstrapAdmin(),
    )

    data class Jwt(
        val issuer: String = "https://api.coding4world.com/bdi",
        val audience: String = "bdi-api",
        val accessTokenTtl: Duration = Duration.ofMinutes(15),
        val publicKey: String = "",
        val privateKey: String = "",
    )

    data class BootstrapAdmin(
        val email: String = "",
        val password: String = "",
    )

    data class OpenApi(
        val public: Boolean = false,
    )
}
