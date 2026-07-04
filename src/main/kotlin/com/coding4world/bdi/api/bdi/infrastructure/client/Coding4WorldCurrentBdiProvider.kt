package com.coding4world.bdi.api.bdi.infrastructure.client

import com.coding4world.bdi.Coding4WorldBdiClient
import com.coding4world.bdi.api.bdi.domain.model.BdiPublication
import com.coding4world.bdi.api.bdi.domain.port.CurrentBdiProvider
import org.springframework.stereotype.Component

@Component
internal class Coding4WorldCurrentBdiProvider(
    private val client: Coding4WorldBdiClient,
) : CurrentBdiProvider {
    override fun current(): BdiPublication {
        val result = client.current()
        return BdiPublication(
            value = result.value,
            validFrom = result.validFrom,
            sourcePdf = result.sourcePdf,
            fetchedAt = result.fetchedAt,
        )
    }
}
