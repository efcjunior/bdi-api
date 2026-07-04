package com.coding4world.bdi.api.bdi.infrastructure.persistence

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = BdiRefreshJobDocument.COLLECTION)
data class BdiRefreshJobDocument(
    @Id val id: String? = null,
    val status: BdiRefreshJobStatus,
    val trigger: BdiRefreshTrigger,
    val requestedBy: String? = null,
    val snapshotId: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val expiresAt: Instant,
    @CreatedDate var createdAt: Instant? = null,
    @LastModifiedDate var updatedAt: Instant? = null,
) {
    companion object {
        const val COLLECTION = "bdi_refresh_jobs"
    }
}
