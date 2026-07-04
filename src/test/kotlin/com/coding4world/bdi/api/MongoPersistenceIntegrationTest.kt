package com.coding4world.bdi.api

import com.coding4world.bdi.api.auth.domain.model.RefreshToken
import com.coding4world.bdi.api.auth.domain.port.RefreshTokenRepository
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
import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.model.UserRole
import com.coding4world.bdi.api.user.domain.port.UserRepository
import com.coding4world.bdi.api.user.infrastructure.persistence.UserDocument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Import
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
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var refreshJobProcessor: BdiRefreshJobProcessor

    @Test
    fun `application context starts with MongoDB`() {
        assertThat(mongoTemplate.db.name).isNotBlank()
    }

    @Test
    fun `repositories persist and retrieve domain models`() {
        val now = Instant.parse("2026-07-04T12:00:00Z")
        val user =
            userRepository.save(
                User(
                    normalizedEmail = "admin@example.com",
                    passwordHash = "encoded-password",
                    roles = setOf(UserRole.ADMIN),
                    enabled = true,
                ),
            )
        val token =
            refreshTokenRepository.save(
                RefreshToken(
                    tokenHash = "token-hash",
                    familyId = "family-id",
                    userId = requireNotNull(user.id),
                    expiresAt = now.plusSeconds(3600),
                ),
            )
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
                    requestedBy = user.id,
                    snapshotId = snapshot.id,
                    expiresAt = now.plusSeconds(86_400),
                ),
            )

        assertThat(userRepository.findByNormalizedEmail("admin@example.com")).isEqualTo(user)
        assertThat(refreshTokenRepository.findByTokenHash("token-hash")).isEqualTo(token)
        assertThat(snapshotRepository.findLatest()).isEqualTo(snapshot)
        assertThat(refreshJobRepository.findById(requireNotNull(job.id))).isEqualTo(job)
    }

    @Test
    fun `required unique and TTL indexes exist`() {
        assertThat(indexNames(UserDocument::class.java)).contains("uk_users_normalized_email")
        assertThat(indexNames(BdiSnapshotDocument::class.java))
            .contains("uk_bdi_snapshots_fingerprint", "ix_bdi_snapshots_latest")
        assertThat(indexNames(BdiRefreshJobDocument::class.java))
            .contains("ix_bdi_refresh_jobs_active", "ttl_bdi_refresh_jobs_expires_at")

        val tokenIndexes = mongoTemplate.indexOps("refresh_tokens").indexInfo
        assertThat(tokenIndexes.map { it.name })
            .contains("uk_refresh_tokens_token_hash", "ix_refresh_tokens_family_id", "ttl_refresh_tokens_expires_at")
        assertThat(tokenIndexes.single { it.name == "ttl_refresh_tokens_expires_at" }.expireAfter)
            .hasValue(java.time.Duration.ZERO)
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
