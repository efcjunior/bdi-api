package com.coding4world.bdi.api.bdi.infrastructure.web

import com.coding4world.bdi.api.bdi.application.BdiUnavailableException
import com.coding4world.bdi.api.bdi.application.GetBdiHistory
import com.coding4world.bdi.api.bdi.application.GetCurrentBdi
import com.coding4world.bdi.api.bdi.domain.model.BdiFreshnessStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

@Validated
@RestController
@RequestMapping("/api/v1/bdi")
class BdiController(
    private val getCurrentBdi: GetCurrentBdi,
    private val getBdiHistory: GetBdiHistory,
) {
    @GetMapping("/current")
    fun current(): CurrentBdiResponse {
        val current = getCurrentBdi.execute() ?: throw BdiUnavailableException()
        return CurrentBdiResponse(
            value = current.snapshot.value,
            validFrom = current.snapshot.validFrom,
            sourcePdf = current.snapshot.sourcePdf,
            lastVerifiedAt = current.snapshot.lastVerifiedAt,
            status = current.status,
        )
    }

    @GetMapping("/history")
    fun history(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): BdiHistoryResponse {
        val result = getBdiHistory.execute(page, size)
        return BdiHistoryResponse(
            content = result.content.map(BdiHistoryItemResponse::from),
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }
}

data class CurrentBdiResponse(
    val value: BigDecimal,
    val unit: String = "PERCENT",
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val lastVerifiedAt: Instant,
    val status: BdiFreshnessStatus,
)

data class BdiHistoryResponse(
    val content: List<BdiHistoryItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class BdiHistoryItemResponse(
    val value: BigDecimal,
    val unit: String = "PERCENT",
    val validFrom: LocalDate,
    val sourcePdf: URI,
    val lastVerifiedAt: Instant,
) {
    companion object {
        fun from(snapshot: BdiSnapshot) =
            BdiHistoryItemResponse(
                value = snapshot.value,
                validFrom = snapshot.validFrom,
                sourcePdf = snapshot.sourcePdf,
                lastVerifiedAt = snapshot.lastVerifiedAt,
            )
    }
}
