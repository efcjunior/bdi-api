package com.coding4world.bdi.api.bdi.domain.model

import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

data class BdiSnapshot(
    val id: String? = null,
    val value: BigDecimal,
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val fingerprint: String,
    val lastVerifiedAt: Instant,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
