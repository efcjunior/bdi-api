package com.coding4world.bdi.api.bdi.infrastructure.web

import com.coding4world.bdi.api.bdi.application.BdiRefreshJobs
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class BdiRefreshJobControllerTest {
    private val service = StubBdiRefreshJobs()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(BdiRefreshJobController(service)).build()
    }

    @Test
    fun `job status endpoint returns the persisted job`() {
        service.jobs["job-1"] =
            BdiRefreshJob(
                id = "job-1",
                status = BdiRefreshJobStatus.SUCCEEDED,
                trigger = BdiRefreshTrigger.ADMIN,
                snapshotId = "snapshot-1",
                startedAt = Instant.parse("2026-07-09T12:00:00Z"),
                completedAt = Instant.parse("2026-07-09T12:00:01Z"),
                expiresAt = Instant.parse("2026-07-16T12:00:00Z"),
            )

        mockMvc.perform(get("/api/v1/admin/bdi/refresh/job-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("job-1"))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.snapshotId").value("snapshot-1"))
    }

    @Test
    fun `job status endpoint returns not found for an unknown job`() {
        mockMvc.perform(get("/api/v1/admin/bdi/refresh/missing"))
            .andExpect(status().isNotFound)
    }
}

private class StubBdiRefreshJobs : BdiRefreshJobs {
    val jobs = mutableMapOf<String, BdiRefreshJob>()

    override fun request(
        trigger: BdiRefreshTrigger,
        requestedBy: String?,
    ): BdiRefreshJob = error("Refresh requests are not used by this test")

    override fun findById(jobId: String): BdiRefreshJob? = jobs[jobId]

    override fun recoverOrphanedJobs(): Int = 0
}
