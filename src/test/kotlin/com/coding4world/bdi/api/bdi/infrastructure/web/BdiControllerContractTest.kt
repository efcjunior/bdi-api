package com.coding4world.bdi.api.bdi.infrastructure.web

import com.coding4world.bdi.api.bdi.application.GetBdiHistory
import com.coding4world.bdi.api.bdi.application.GetCurrentBdi
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.shared.domain.PageResult
import com.coding4world.bdi.api.shared.web.ApiExceptionHandler
import com.coding4world.bdi.api.shared.web.ApiProblemFactory
import com.coding4world.bdi.api.shared.web.TraceIdFilter
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BdiControllerContractTest {
    private val now = Instant.parse("2026-07-14T12:00:00Z")

    @Test
    fun `current endpoint returns the documented contract`() {
        val mockMvc = mockMvc(listOf(snapshot("snapshot-1", "35.08", "2026-01-15", now.minusSeconds(3600))))

        mockMvc.perform(get("/api/v1/bdi/current"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.value").value(35.08))
            .andExpect(jsonPath("$.unit").value("PERCENT"))
            .andExpect(jsonPath("$.validFrom").value("2026-01-15"))
            .andExpect(jsonPath("$.sourcePdf").value("https://example.com/snapshot-1.pdf"))
            .andExpect(jsonPath("$.status").value("CURRENT"))
    }

    @Test
    fun `missing snapshot returns service unavailable problem details`() {
        val mockMvc = mockMvc(emptyList())

        mockMvc.perform(get("/api/v1/bdi/current"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(jsonPath("$.code").value("BDI_UNAVAILABLE"))
            .andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test
    fun `history endpoint returns stable pagination metadata`() {
        val snapshots =
            listOf(
                snapshot("snapshot-2", "36.00", "2026-02-15", now),
                snapshot("snapshot-1", "35.08", "2026-01-15", now.minusSeconds(3600)),
            )
        val mockMvc = mockMvc(snapshots)

        mockMvc.perform(get("/api/v1/bdi/history").param("page", "1").param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].value").value(35.08))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
    }

    @Test
    fun `oversized history page returns validation problem`() {
        mockMvc(emptyList()).perform(get("/api/v1/bdi/history").param("size", "101"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    private fun mockMvc(snapshots: List<BdiSnapshot>): MockMvc {
        val repository = ContractBdiSnapshotRepository(snapshots)
        val controller =
            BdiController(
                GetCurrentBdi(repository, BdiApiProperties(), Clock.fixed(now, ZoneOffset.UTC)),
                GetBdiHistory(repository),
            )
        return MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(ApiExceptionHandler(ApiProblemFactory()))
            .addFilters<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(TraceIdFilter())
            .build()
    }

    private fun snapshot(
        id: String,
        value: String,
        validFrom: String,
        lastVerifiedAt: Instant,
    ) =
        BdiSnapshot(
            id = id,
            value = BigDecimal(value),
            validFrom = LocalDate.parse(validFrom),
            sourcePdf = URI("https://example.com/$id.pdf"),
            fingerprint = "$id-fingerprint",
            lastVerifiedAt = lastVerifiedAt,
        )
}

private class ContractBdiSnapshotRepository(
    private val snapshots: List<BdiSnapshot>,
) : BdiSnapshotRepository {
    override fun save(snapshot: BdiSnapshot): BdiSnapshot = snapshot

    override fun findLatest(): BdiSnapshot? = snapshots.firstOrNull()

    override fun findByFingerprint(fingerprint: String): BdiSnapshot? =
        snapshots.firstOrNull { it.fingerprint == fingerprint }

    override fun findHistory(
        page: Int,
        size: Int,
    ): PageResult<BdiSnapshot> {
        val fromIndex = (page * size).coerceAtMost(snapshots.size)
        val toIndex = (fromIndex + size).coerceAtMost(snapshots.size)
        val totalPages = if (snapshots.isEmpty()) 0 else (snapshots.size + size - 1) / size
        return PageResult(snapshots.subList(fromIndex, toIndex), page, size, snapshots.size.toLong(), totalPages)
    }
}
