package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiFreshnessStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.shared.domain.PageResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GetCurrentBdiTest {
    private val now = Instant.parse("2026-07-08T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val properties =
        BdiApiProperties(
            synchronization = BdiApiProperties.Synchronization(staleAfter = Duration.ofHours(12)),
        )

    @Test
    fun `recently verified snapshot is current`() {
        val useCase = useCaseWith(snapshot(lastVerifiedAt = now.minus(Duration.ofHours(11))))

        assertThat(useCase.execute()?.status).isEqualTo(BdiFreshnessStatus.CURRENT)
    }

    @Test
    fun `snapshot at the freshness boundary is current`() {
        val useCase = useCaseWith(snapshot(lastVerifiedAt = now.minus(Duration.ofHours(12))))

        assertThat(useCase.execute()?.status).isEqualTo(BdiFreshnessStatus.CURRENT)
    }

    @Test
    fun `snapshot older than the freshness boundary is stale`() {
        val useCase = useCaseWith(snapshot(lastVerifiedAt = now.minus(Duration.ofHours(12)).minusMillis(1)))

        assertThat(useCase.execute()?.status).isEqualTo(BdiFreshnessStatus.STALE)
    }

    @Test
    fun `missing snapshot returns no current BDI`() {
        assertThat(useCaseWith(null).execute()).isNull()
    }

    private fun useCaseWith(snapshot: BdiSnapshot?): GetCurrentBdi =
        GetCurrentBdi(
            snapshotRepository = StubBdiSnapshotRepository(snapshot),
            properties = properties,
            clock = clock,
        )

    private fun snapshot(lastVerifiedAt: Instant) =
        BdiSnapshot(
            id = "snapshot-1",
            value = BigDecimal("35.08"),
            validFrom = LocalDate.parse("2026-01-15"),
            sourcePdf = URI("https://example.com/bdi.pdf"),
            fingerprint = "fingerprint",
            lastVerifiedAt = lastVerifiedAt,
        )
}

private class StubBdiSnapshotRepository(
    private val snapshot: BdiSnapshot?,
) : BdiSnapshotRepository {
    override fun save(snapshot: BdiSnapshot): BdiSnapshot = snapshot

    override fun findLatest(): BdiSnapshot? = snapshot

    override fun findByFingerprint(fingerprint: String): BdiSnapshot? = null

    override fun findHistory(
        page: Int,
        size: Int,
    ) = PageResult(listOfNotNull(snapshot), page, size, if (snapshot == null) 0 else 1, if (snapshot == null) 0 else 1)
}
