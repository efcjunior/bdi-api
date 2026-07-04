package com.coding4world.bdi.api.bdi.domain.model

data class CurrentBdi(
    val snapshot: BdiSnapshot,
    val status: BdiFreshnessStatus,
)

enum class BdiFreshnessStatus {
    CURRENT,
    STALE,
}
