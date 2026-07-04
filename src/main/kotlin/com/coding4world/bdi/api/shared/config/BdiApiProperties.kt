package com.coding4world.bdi.api.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("bdi-api")
data class BdiApiProperties(
    val synchronization: Synchronization = Synchronization(),
) {
    data class Synchronization(
        val enabled: Boolean = true,
        val interval: Duration = Duration.ofHours(6),
        val staleAfter: Duration = Duration.ofHours(12),
        val jobRetention: Duration = Duration.ofDays(7),
    )
}
