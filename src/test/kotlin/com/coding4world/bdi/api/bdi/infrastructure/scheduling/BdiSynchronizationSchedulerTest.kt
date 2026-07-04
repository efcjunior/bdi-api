package com.coding4world.bdi.api.bdi.infrastructure.scheduling

import com.coding4world.bdi.api.bdi.application.BdiRefreshJobs
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant

class BdiSynchronizationSchedulerTest {
    private val service = RecordingBdiRefreshJobs()
    private val scheduler = BdiSynchronizationScheduler(service)

    @Test
    fun `startup recovers interrupted jobs before requesting refresh`() {
        val job = pendingJob(BdiRefreshTrigger.STARTUP)
        service.recoveredJobCount = 1
        service.jobToReturn = job

        scheduler.refreshAtStartup()

        assertThat(service.calls).containsExactly("recover", "request:STARTUP")
    }

    @Test
    fun `scheduled invocation requests a scheduled refresh`() {
        service.jobToReturn = pendingJob(BdiRefreshTrigger.SCHEDULED)

        scheduler.refreshOnSchedule()

        assertThat(service.calls).containsExactly("request:SCHEDULED")
    }

    private fun pendingJob(trigger: BdiRefreshTrigger) =
        BdiRefreshJob(
            id = "job-1",
            status = BdiRefreshJobStatus.PENDING,
            trigger = trigger,
            expiresAt = Instant.parse("2026-07-16T12:00:00Z"),
        )
}

private class RecordingBdiRefreshJobs : BdiRefreshJobs {
    val calls = mutableListOf<String>()
    var recoveredJobCount = 0
    lateinit var jobToReturn: BdiRefreshJob

    override fun request(
        trigger: BdiRefreshTrigger,
        requestedBy: String?,
    ): BdiRefreshJob {
        calls += "request:$trigger"
        return jobToReturn
    }

    override fun findById(jobId: String): BdiRefreshJob? = null

    override fun recoverOrphanedJobs(): Int {
        calls += "recover"
        return recoveredJobCount
    }
}
