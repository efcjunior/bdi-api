package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiPublication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

class BdiFingerprintGeneratorTest {
    private val generator = BdiFingerprintGenerator()

    @Test
    fun `equivalent publication content produces the same fingerprint`() {
        val first = publication(value = BigDecimal("35.080"), sourcePdf = URI("https://example.com/a/../bdi.pdf"))
        val second =
            publication(
                value = BigDecimal("35.08"),
                sourcePdf = URI("https://example.com/bdi.pdf"),
                fetchedAt = Instant.parse("2026-07-08T18:00:00Z"),
            )

        assertThat(generator.generate(first)).isEqualTo(generator.generate(second))
        assertThat(generator.generate(first)).matches("[0-9a-f]{64}")
    }

    @Test
    fun `changed publication content produces a different fingerprint`() {
        val original = publication()

        assertThat(generator.generate(publication(value = BigDecimal("35.09"))))
            .isNotEqualTo(generator.generate(original))
        assertThat(generator.generate(publication(validFrom = LocalDate.parse("2026-02-01"))))
            .isNotEqualTo(generator.generate(original))
        assertThat(generator.generate(publication(sourcePdf = URI("https://example.com/revised.pdf"))))
            .isNotEqualTo(generator.generate(original))
    }

    private fun publication(
        value: BigDecimal = BigDecimal("35.08"),
        validFrom: LocalDate = LocalDate.parse("2026-01-15"),
        sourcePdf: URI = URI("https://example.com/bdi.pdf"),
        fetchedAt: Instant = Instant.parse("2026-07-08T12:00:00Z"),
    ) = BdiPublication(value, validFrom, sourcePdf, fetchedAt)
}
