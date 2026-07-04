package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiFreshnessStatus
import com.coding4world.bdi.api.bdi.domain.model.CurrentBdi
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class GetCurrentBdi(
    private val snapshotRepository: BdiSnapshotRepository,
    private val properties: BdiApiProperties,
    private val clock: Clock,
) {
    fun execute(): CurrentBdi? {
        val snapshot = snapshotRepository.findLatest() ?: return null
        val staleThreshold = clock.instant().minus(properties.synchronization.staleAfter)
        val status =
            if (snapshot.lastVerifiedAt.isBefore(staleThreshold)) {
                BdiFreshnessStatus.STALE
            } else {
                BdiFreshnessStatus.CURRENT
            }

        return CurrentBdi(snapshot, status)
    }
}
