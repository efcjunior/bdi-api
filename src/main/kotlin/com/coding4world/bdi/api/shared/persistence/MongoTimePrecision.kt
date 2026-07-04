package com.coding4world.bdi.api.shared.persistence

import java.time.Instant
import java.time.temporal.ChronoUnit

internal fun Instant.toMongoPrecision(): Instant = truncatedTo(ChronoUnit.MILLIS)
