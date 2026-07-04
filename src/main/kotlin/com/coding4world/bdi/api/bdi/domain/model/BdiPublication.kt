package com.coding4world.bdi.api.bdi.domain.model

import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

/** Data obtained from the currently published BDI source. */
data class BdiPublication(
    val value: BigDecimal,
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val fetchedAt: Instant,
)
