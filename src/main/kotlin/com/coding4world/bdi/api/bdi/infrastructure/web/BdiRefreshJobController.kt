package com.coding4world.bdi.api.bdi.infrastructure.web

import com.coding4world.bdi.api.bdi.application.BdiRefreshJobs
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/api/v1/admin/bdi/refresh")
class BdiRefreshJobController(
    private val refreshJobs: BdiRefreshJobs,
) {
    @GetMapping("/{jobId}")
    fun getJob(
        @PathVariable jobId: String,
    ): BdiRefreshJobResponse {
        val job =
            refreshJobs.findById(jobId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BDI refresh job not found")
        return BdiRefreshJobResponse.from(job)
    }
}

data class BdiRefreshJobResponse(
    val id: String,
    val status: BdiRefreshJobStatus,
    val trigger: BdiRefreshTrigger,
    val requestedBy: String?,
    val snapshotId: String?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val errorCode: String?,
    val errorMessage: String?,
) {
    companion object {
        fun from(job: BdiRefreshJob) =
            BdiRefreshJobResponse(
                id = requireNotNull(job.id),
                status = job.status,
                trigger = job.trigger,
                requestedBy = job.requestedBy,
                snapshotId = job.snapshotId,
                startedAt = job.startedAt,
                completedAt = job.completedAt,
                errorCode = job.errorCode,
                errorMessage = job.errorMessage,
            )
    }
}
