package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiPublication
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.bdi.domain.port.CurrentBdiProvider
import com.coding4world.bdi.api.shared.domain.PageResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

class SynchronizeCurrentBdiTest {
    private val repository = InMemoryBdiSnapshotRepository()
    private val fingerprintGenerator = BdiFingerprintGenerator()

    @Test
    fun `new publication creates a snapshot`() {
        val publication = publication()
        val useCase = useCaseFor(publication)

        val result = useCase.execute()

        assertThat(result.outcome).isEqualTo(BdiSynchronizationOutcome.CREATED)
        assertThat(result.snapshot.id).isNotNull()
        assertThat(result.snapshot.lastVerifiedAt).isEqualTo(publication.fetchedAt)
        assertThat(repository.snapshots).hasSize(1)
    }

    @Test
    fun `unchanged publication only advances its verification time`() {
        val firstPublication = publication()
        val firstResult = useCaseFor(firstPublication).execute()
        val laterFetch = Instant.parse("2026-07-08T18:00:00Z")

        val result = useCaseFor(firstPublication.copy(fetchedAt = laterFetch)).execute()

        assertThat(result.outcome).isEqualTo(BdiSynchronizationOutcome.VERIFIED)
        assertThat(result.snapshot.id).isEqualTo(firstResult.snapshot.id)
        assertThat(result.snapshot.lastVerifiedAt).isEqualTo(laterFetch)
        assertThat(repository.snapshots).hasSize(1)
    }

    @Test
    fun `changed publication creates another snapshot`() {
        useCaseFor(publication()).execute()

        val result = useCaseFor(publication(value = BigDecimal("36.10"))).execute()

        assertThat(result.outcome).isEqualTo(BdiSynchronizationOutcome.CREATED)
        assertThat(repository.snapshots).hasSize(2)
    }

    private fun useCaseFor(publication: BdiPublication) =
        SynchronizeCurrentBdi(
            currentBdiProvider = CurrentBdiProvider { publication },
            snapshotRepository = repository,
            fingerprintGenerator = fingerprintGenerator,
        )

    private fun publication(
        value: BigDecimal = BigDecimal("35.08"),
        fetchedAt: Instant = Instant.parse("2026-07-08T12:00:00Z"),
    ) =
        BdiPublication(
            value = value,
            validFrom = LocalDate.parse("2026-01-15"),
            sourcePdf = URI("https://example.com/bdi.pdf"),
            fetchedAt = fetchedAt,
        )
}

private class InMemoryBdiSnapshotRepository : BdiSnapshotRepository {
    val snapshots = mutableListOf<BdiSnapshot>()

    override fun save(snapshot: BdiSnapshot): BdiSnapshot {
        val saved = snapshot.copy(id = snapshot.id ?: "snapshot-${snapshots.size + 1}")
        snapshots.removeIf { it.id == saved.id }
        snapshots += saved
        return saved
    }

    override fun findLatest(): BdiSnapshot? = snapshots.maxByOrNull { it.validFrom }

    override fun findByFingerprint(fingerprint: String): BdiSnapshot? =
        snapshots.firstOrNull { it.fingerprint == fingerprint }

    override fun findHistory(
        page: Int,
        size: Int,
    ) = PageResult(snapshots, page, size, snapshots.size.toLong(), if (snapshots.isEmpty()) 0 else 1)
}
