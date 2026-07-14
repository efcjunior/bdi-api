package com.coding4world.bdi.api.bdi.infrastructure.persistence

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiRefreshJobRepository
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.shared.persistence.toMongoPrecision
import com.coding4world.bdi.api.shared.domain.PageResult
import org.bson.types.Decimal128
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
internal class MongoBdiSnapshotRepository(
    private val repository: SpringDataBdiSnapshotRepository,
) : BdiSnapshotRepository {
    override fun save(snapshot: BdiSnapshot): BdiSnapshot = repository.save(snapshot.toDocument()).toDomain()

    override fun findLatest(): BdiSnapshot? = repository.findTopByOrderByValidFromDescCreatedAtDesc()?.toDomain()

    override fun findByFingerprint(fingerprint: String): BdiSnapshot? = repository.findByFingerprint(fingerprint)?.toDomain()

    override fun findHistory(
        page: Int,
        size: Int,
    ): PageResult<BdiSnapshot> {
        val result = repository.findAllByOrderByValidFromDescCreatedAtDesc(PageRequest.of(page, size))
        return PageResult(
            content = result.content.map(BdiSnapshotDocument::toDomain),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

@Repository
internal class MongoBdiRefreshJobRepository(
    private val repository: SpringDataBdiRefreshJobRepository,
) : BdiRefreshJobRepository {
    override fun save(job: BdiRefreshJob): BdiRefreshJob = repository.save(job.toDocument()).toDomain()

    override fun findById(id: String): BdiRefreshJob? = repository.findByIdOrNull(id)?.toDomain()

    override fun findActive(statuses: Set<BdiRefreshJobStatus>): BdiRefreshJob? =
        repository.findTopByStatusInOrderByCreatedAtAsc(statuses)?.toDomain()
}

private fun BdiSnapshot.toDocument() =
    BdiSnapshotDocument(
        id = id,
        value = Decimal128(value),
        validFrom = validFrom,
        sourcePdf = sourcePdf,
        fingerprint = fingerprint,
        lastVerifiedAt = lastVerifiedAt.toMongoPrecision(),
        createdAt = createdAt?.toMongoPrecision(),
        updatedAt = updatedAt?.toMongoPrecision(),
    )

private fun BdiSnapshotDocument.toDomain() =
    BdiSnapshot(
        id = id,
        value = value.bigDecimalValue(),
        validFrom = validFrom,
        sourcePdf = sourcePdf,
        fingerprint = fingerprint,
        lastVerifiedAt = lastVerifiedAt.toMongoPrecision(),
        createdAt = createdAt?.toMongoPrecision(),
        updatedAt = updatedAt?.toMongoPrecision(),
    )

private fun BdiRefreshJob.toDocument() =
    BdiRefreshJobDocument(
        id = id,
        status = status,
        trigger = trigger,
        requestedBy = requestedBy,
        snapshotId = snapshotId,
        startedAt = startedAt?.toMongoPrecision(),
        completedAt = completedAt?.toMongoPrecision(),
        errorCode = errorCode,
        errorMessage = errorMessage,
        expiresAt = expiresAt.toMongoPrecision(),
        createdAt = createdAt?.toMongoPrecision(),
        updatedAt = updatedAt?.toMongoPrecision(),
    )

private fun BdiRefreshJobDocument.toDomain() =
    BdiRefreshJob(
        id = id,
        status = status,
        trigger = trigger,
        requestedBy = requestedBy,
        snapshotId = snapshotId,
        startedAt = startedAt?.toMongoPrecision(),
        completedAt = completedAt?.toMongoPrecision(),
        errorCode = errorCode,
        errorMessage = errorMessage,
        expiresAt = expiresAt.toMongoPrecision(),
        createdAt = createdAt?.toMongoPrecision(),
        updatedAt = updatedAt?.toMongoPrecision(),
    )
