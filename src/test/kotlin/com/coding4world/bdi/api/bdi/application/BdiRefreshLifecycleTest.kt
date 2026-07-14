package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiPublication
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiRefreshJobRepository
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.bdi.domain.port.CurrentBdiProvider
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.shared.domain.PageResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.math.BigDecimal
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class BdiRefreshLifecycleTest {
    private val now = Instant.parse("2026-07-09T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `refresh job completes successfully and references its snapshot`() {
        val fixture = fixture(CurrentBdiProvider { publication() })

        val requestedJob = fixture.service.request(BdiRefreshTrigger.SCHEDULED)
        val requestedJobId = requireNotNull(requestedJob.id)
        assertThat(fixture.jobs.findById(requestedJobId)?.status)
            .isEqualTo(BdiRefreshJobStatus.PENDING)

        fixture.executor.runNext()

        val completedJob = fixture.jobs.findById(requestedJobId)
        assertThat(completedJob?.status).isEqualTo(BdiRefreshJobStatus.SUCCEEDED)
        assertThat(completedJob?.startedAt).isEqualTo(now)
        assertThat(completedJob?.completedAt).isEqualTo(now)
        assertThat(completedJob?.snapshotId).isNotBlank()
        assertThat(completedJob?.errorCode).isNull()
    }

    @Test
    fun `source failure stores a safe error and preserves existing snapshots`() {
        val fixture =
            fixture(
                CurrentBdiProvider {
                    throw IllegalStateException("Sensitive upstream details")
                },
            )
        fixture.snapshots.save(snapshot(id = "existing-snapshot"))
        val requestedJob = fixture.service.request(BdiRefreshTrigger.STARTUP)

        fixture.executor.runNext()

        val failedJob = fixture.jobs.findById(requestedJob.id!!)
        assertThat(failedJob?.status).isEqualTo(BdiRefreshJobStatus.FAILED)
        assertThat(failedJob?.errorCode).isEqualTo("BDI_SOURCE_REFRESH_FAILED")
        assertThat(failedJob?.errorMessage).isEqualTo("The external BDI source could not be refreshed")
        assertThat(failedJob?.errorMessage).doesNotContain("Sensitive")
        assertThat(fixture.snapshots.snapshots).hasSize(1)
    }

    @Test
    fun `another refresh is rejected while a job is active`() {
        val fixture = fixture(CurrentBdiProvider { publication() })
        val activeJob = fixture.service.request(BdiRefreshTrigger.ADMIN, "admin-1")

        val exception =
            assertThrows<BdiRefreshAlreadyRunningException> {
                fixture.service.request(BdiRefreshTrigger.ADMIN, "admin-2")
            }

        assertThat(exception.activeJobId).isEqualTo(activeJob.id)
        assertThat(fixture.executor.tasks).hasSize(1)
    }

    @Test
    fun `new jobs receive the configured retention period`() {
        val fixture = fixture(CurrentBdiProvider { publication() })

        val job = fixture.service.request(BdiRefreshTrigger.SCHEDULED)

        assertThat(job.expiresAt).isEqualTo(now.plus(Duration.ofDays(7)))
        assertThat(fixture.service.findById(job.id!!)).isEqualTo(job)
    }

    @Test
    fun `orphaned jobs are failed before a replacement is requested`() {
        val fixture = fixture(CurrentBdiProvider { publication() })
        val orphanedJob = fixture.service.request(BdiRefreshTrigger.STARTUP)

        val recoveredCount = fixture.service.recoverOrphanedJobs()
        val replacementJob = fixture.service.request(BdiRefreshTrigger.STARTUP)

        val recoveredJob = fixture.jobs.findById(orphanedJob.id!!)
        assertThat(recoveredCount).isEqualTo(1)
        assertThat(recoveredJob?.status).isEqualTo(BdiRefreshJobStatus.FAILED)
        assertThat(recoveredJob?.errorCode).isEqualTo("BDI_REFRESH_INTERRUPTED")
        assertThat(replacementJob.id).isNotEqualTo(orphanedJob.id)
    }

    @Test
    fun `executor rejection fails the persisted pending job`() {
        val jobs = InMemoryBdiRefreshJobRepository()
        val snapshots = RefreshInMemoryBdiSnapshotRepository()
        val synchronization =
            SynchronizeCurrentBdi(CurrentBdiProvider { publication() }, snapshots, BdiFingerprintGenerator())
        val processor = BdiRefreshJobProcessor(jobs, synchronization, clock)
        val rejectingExecutor = TaskExecutor { throw TaskRejectedException("Executor is unavailable") }
        val service = createService(jobs, processor, rejectingExecutor)

        val exception =
            assertThrows<BdiRefreshSchedulingException> {
                service.request(BdiRefreshTrigger.SCHEDULED)
            }

        val failedJob = jobs.findById(exception.jobId)
        assertThat(failedJob?.status).isEqualTo(BdiRefreshJobStatus.FAILED)
        assertThat(failedJob?.errorCode).isEqualTo("BDI_REFRESH_SCHEDULING_FAILED")
        assertThat(jobs.findActive(setOf(BdiRefreshJobStatus.PENDING, BdiRefreshJobStatus.RUNNING))).isNull()
    }

    private fun fixture(provider: CurrentBdiProvider): RefreshFixture {
        val jobs = InMemoryBdiRefreshJobRepository()
        val snapshots = RefreshInMemoryBdiSnapshotRepository()
        val executor = CapturingTaskExecutor()
        val synchronization = SynchronizeCurrentBdi(provider, snapshots, BdiFingerprintGenerator())
        val processor = BdiRefreshJobProcessor(jobs, synchronization, clock)
        val service = createService(jobs, processor, executor)
        return RefreshFixture(service, jobs, snapshots, executor)
    }

    private fun createService(
        jobs: BdiRefreshJobRepository,
        processor: BdiRefreshJobProcessor,
        executor: TaskExecutor,
    ) =
        BdiRefreshJobService(
            jobRepository = jobs,
            processor = processor,
            properties = BdiApiProperties(),
            clock = clock,
            taskExecutor = executor,
        )

    private fun publication() =
        BdiPublication(
            value = BigDecimal("35.08"),
            validFrom = LocalDate.parse("2026-01-15"),
            sourcePdf = URI("https://example.com/bdi.pdf"),
            fetchedAt = now,
        )

    private fun snapshot(id: String) =
        BdiSnapshot(
            id = id,
            value = BigDecimal("34.00"),
            validFrom = LocalDate.parse("2025-01-15"),
            sourcePdf = URI("https://example.com/old.pdf"),
            fingerprint = "old-fingerprint",
            lastVerifiedAt = now.minus(Duration.ofDays(1)),
        )
}

private data class RefreshFixture(
    val service: BdiRefreshJobService,
    val jobs: InMemoryBdiRefreshJobRepository,
    val snapshots: RefreshInMemoryBdiSnapshotRepository,
    val executor: CapturingTaskExecutor,
)

private class CapturingTaskExecutor : TaskExecutor {
    val tasks = ArrayDeque<Runnable>()

    override fun execute(task: Runnable) {
        tasks.addLast(task)
    }

    fun runNext() {
        tasks.removeFirst().run()
    }
}

private class InMemoryBdiRefreshJobRepository : BdiRefreshJobRepository {
    private val jobs = linkedMapOf<String, BdiRefreshJob>()

    override fun save(job: BdiRefreshJob): BdiRefreshJob {
        val saved = job.copy(id = job.id ?: "job-${jobs.size + 1}")
        jobs[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findById(id: String): BdiRefreshJob? = jobs[id]

    override fun findActive(statuses: Set<BdiRefreshJobStatus>): BdiRefreshJob? =
        jobs.values.firstOrNull { it.status in statuses }
}

private class RefreshInMemoryBdiSnapshotRepository : BdiSnapshotRepository {
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
