package com.coding4world.bdi.api.bdi.infrastructure.persistence

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

internal interface SpringDataBdiSnapshotRepository : MongoRepository<BdiSnapshotDocument, String> {
    fun findTopByOrderByValidFromDescCreatedAtDesc(): BdiSnapshotDocument?

    fun findByFingerprint(fingerprint: String): BdiSnapshotDocument?

    fun findAllByOrderByValidFromDescCreatedAtDesc(pageable: Pageable): Page<BdiSnapshotDocument>
}

internal interface SpringDataBdiRefreshJobRepository : MongoRepository<BdiRefreshJobDocument, String> {
    fun findTopByStatusInOrderByCreatedAtAsc(statuses: Collection<BdiRefreshJobStatus>): BdiRefreshJobDocument?
}
