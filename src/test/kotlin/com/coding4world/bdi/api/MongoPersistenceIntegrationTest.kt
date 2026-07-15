package com.coding4world.bdi.api

import com.coding4world.bdi.api.bdi.application.BdiRefreshJobProcessor
import com.coding4world.bdi.api.bdi.domain.model.BdiPublication
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiRefreshJobRepository
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.bdi.domain.port.CurrentBdiProvider
import com.coding4world.bdi.api.bdi.infrastructure.persistence.BdiRefreshJobDocument
import com.coding4world.bdi.api.bdi.infrastructure.persistence.BdiSnapshotDocument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mongodb.MongoDBContainer
import java.math.BigDecimal
import java.net.URI
import java.time.Instant
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@Import(MongoPersistenceIntegrationTest.FakeBdiProviderConfiguration::class)
class MongoPersistenceIntegrationTest {
    @Autowired
    private lateinit var snapshotRepository: BdiSnapshotRepository

    @Autowired
    private lateinit var refreshJobRepository: BdiRefreshJobRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var refreshJobProcessor: BdiRefreshJobProcessor

    @Test
    fun `application context starts with MongoDB`() {
        assertThat(mongoTemplate.db.name).isNotBlank()
    }

    @Test
    fun `repositories persist and retrieve BDI domain models`() {
        val now = Instant.parse("2026-07-04T12:00:00Z")
        val snapshot =
            snapshotRepository.save(
                BdiSnapshot(
                    value = BigDecimal("35.08"),
                    validFrom = LocalDate.parse("2026-01-15"),
                    sourcePdf = URI.create("https://example.com/source.pdf"),
                    fingerprint = "fingerprint",
                    lastVerifiedAt = now,
                ),
            )
        val job =
            refreshJobRepository.save(
                BdiRefreshJob(
                    status = BdiRefreshJobStatus.PENDING,
                    trigger = BdiRefreshTrigger.ADMIN,
                    requestedBy = "admin-user-id",
                    snapshotId = snapshot.id,
                    expiresAt = now.plusSeconds(86_400),
                ),
            )

        assertThat(snapshotRepository.findLatest()).isEqualTo(snapshot)
        assertThat(snapshotRepository.findHistory(0, 10).content).contains(snapshot)
        assertThat(refreshJobRepository.findById(requireNotNull(job.id))).isEqualTo(job)
    }

    @Test
    fun `required BDI unique and TTL indexes exist`() {
        assertThat(indexNames(BdiSnapshotDocument::class.java))
            .contains("uk_bdi_snapshots_fingerprint", "ix_bdi_snapshots_latest")
        assertThat(indexNames(BdiRefreshJobDocument::class.java))
            .contains("ix_bdi_refresh_jobs_active", "ttl_bdi_refresh_jobs_expires_at")
    }

    @Test
    fun `refresh processor persists lifecycle transitions in MongoDB`() {
        val job =
            refreshJobRepository.save(
                BdiRefreshJob(
                    status = BdiRefreshJobStatus.PENDING,
                    trigger = BdiRefreshTrigger.SCHEDULED,
                    expiresAt = Instant.parse("2026-07-16T12:00:00Z"),
                ),
            )

        val jobId = requireNotNull(job.id)
        refreshJobProcessor.process(jobId)

        val completedJob = refreshJobRepository.findById(jobId)
        assertThat(completedJob?.status).isEqualTo(BdiRefreshJobStatus.SUCCEEDED)
        assertThat(completedJob?.startedAt).isNotNull()
        assertThat(completedJob?.completedAt).isNotNull()
        assertThat(completedJob?.snapshotId).isNotBlank()
        assertThat(snapshotRepository.findLatest()?.id).isEqualTo(completedJob?.snapshotId)
    }

    private fun indexNames(type: Class<*>): List<String> = mongoTemplate.indexOps(type).indexInfo.map { it.name }

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mongo = MongoDBContainer("mongo:8.0")
    }

    @TestConfiguration
    class FakeBdiProviderConfiguration {
        @Bean
        @Primary
        fun integrationCurrentBdiProvider(): CurrentBdiProvider =
            CurrentBdiProvider {
                BdiPublication(
                    value = BigDecimal("35.08"),
                    validFrom = LocalDate.parse("2026-01-15"),
                    sourcePdf = URI.create("https://example.com/mongo-integration.pdf"),
                    fetchedAt = Instant.parse("2026-07-09T12:00:00Z"),
                )
            }
    }
}
