package com.coding4world.bdi.api

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class BdiApiApplicationTest {
    @Test
    fun `default synchronization settings match the API policy`() {
        val properties = BdiApiProperties()

        assertThat(properties.synchronization.interval).isEqualTo(Duration.ofHours(6))
        assertThat(properties.synchronization.staleAfter).isEqualTo(Duration.ofHours(12))
    }
}
