package com.coding4world.bdi.api.bdi.infrastructure.persistence

import org.bson.types.Decimal128
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.net.URI
import java.time.Instant
import java.time.LocalDate

@Document(collection = BdiSnapshotDocument.COLLECTION)
data class BdiSnapshotDocument(
    @Id val id: String? = null,
    val value: Decimal128,
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val fingerprint: String,
    val lastVerifiedAt: Instant,
    @CreatedDate var createdAt: Instant? = null,
    @LastModifiedDate var updatedAt: Instant? = null,
) {
    companion object {
        const val COLLECTION = "bdi_snapshots"
    }
}
