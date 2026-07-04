package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.bdi.domain.port.CurrentBdiProvider
import org.springframework.stereotype.Service

@Service
class SynchronizeCurrentBdi(
    private val currentBdiProvider: CurrentBdiProvider,
    private val snapshotRepository: BdiSnapshotRepository,
    private val fingerprintGenerator: BdiFingerprintGenerator,
) {
    fun execute(): BdiSynchronizationResult {
        val publication =
            try {
                currentBdiProvider.current()
            } catch (exception: Exception) {
                throw BdiSourceRefreshException(exception)
            }
        val fingerprint = fingerprintGenerator.generate(publication)
        val existingSnapshot = snapshotRepository.findByFingerprint(fingerprint)

        if (existingSnapshot != null) {
            val verifiedSnapshot =
                snapshotRepository.save(
                    existingSnapshot.copy(lastVerifiedAt = publication.fetchedAt),
                )
            return BdiSynchronizationResult(verifiedSnapshot, BdiSynchronizationOutcome.VERIFIED)
        }

        val createdSnapshot =
            snapshotRepository.save(
                BdiSnapshot(
                    value = publication.value,
                    validFrom = publication.validFrom,
                    sourcePdf = publication.sourcePdf,
                    fingerprint = fingerprint,
                    lastVerifiedAt = publication.fetchedAt,
                ),
            )
        return BdiSynchronizationResult(createdSnapshot, BdiSynchronizationOutcome.CREATED)
    }
}

data class BdiSynchronizationResult(
    val snapshot: BdiSnapshot,
    val outcome: BdiSynchronizationOutcome,
)

enum class BdiSynchronizationOutcome {
    CREATED,
    VERIFIED,
}
