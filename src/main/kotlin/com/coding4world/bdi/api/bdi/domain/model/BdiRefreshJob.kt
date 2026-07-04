package com.coding4world.bdi.api.bdi.domain.model

import java.time.Instant

data class BdiRefreshJob(
    val id: String? = null,
    val status: BdiRefreshJobStatus,
    val trigger: BdiRefreshTrigger,
    val requestedBy: String? = null,
    val snapshotId: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val expiresAt: Instant,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

enum class BdiRefreshJobStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
}

enum class BdiRefreshTrigger {
    STARTUP,
    SCHEDULED,
    ADMIN,
}
