package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import com.coding4world.bdi.api.bdi.domain.port.BdiRefreshJobRepository
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class BdiRefreshJobService(
    private val jobRepository: BdiRefreshJobRepository,
    private val processor: BdiRefreshJobProcessor,
    private val properties: BdiApiProperties,
    private val clock: Clock,
    @param:Qualifier("bdiRefreshTaskExecutor") private val taskExecutor: TaskExecutor,
) : BdiRefreshJobs {
    override fun request(
        trigger: BdiRefreshTrigger,
        requestedBy: String?,
    ): BdiRefreshJob =
        creationLock.withLock {
            val activeJob = jobRepository.findActive(ACTIVE_STATUSES)
            if (activeJob != null) {
                throw BdiRefreshAlreadyRunningException(activeJob.id)
            }

            val now = clock.instant()
            val job =
                jobRepository.save(
                    BdiRefreshJob(
                        status = BdiRefreshJobStatus.PENDING,
                        trigger = trigger,
                        requestedBy = requestedBy,
                        expiresAt = now.plus(properties.synchronization.jobRetention),
                    ),
            )
            val jobId = requireNotNull(job.id) { "The persisted refresh job must have an identifier" }
            try {
                taskExecutor.execute { processor.process(jobId) }
            } catch (exception: TaskRejectedException) {
                jobRepository.save(
                    job.copy(
                        status = BdiRefreshJobStatus.FAILED,
                        completedAt = clock.instant(),
                        errorCode = SCHEDULING_FAILED,
                        errorMessage = SCHEDULING_FAILURE_MESSAGE,
                    ),
                )
                throw BdiRefreshSchedulingException(jobId, exception)
            }
            job
        }

    override fun findById(jobId: String): BdiRefreshJob? = jobRepository.findById(jobId)

    override fun recoverOrphanedJobs(): Int =
        creationLock.withLock {
            var recoveredJobs = 0
            var activeJob = jobRepository.findActive(ACTIVE_STATUSES)
            while (activeJob != null) {
                jobRepository.save(
                    activeJob.copy(
                        status = BdiRefreshJobStatus.FAILED,
                        completedAt = clock.instant(),
                        errorCode = INTERRUPTED,
                        errorMessage = INTERRUPTION_MESSAGE,
                    ),
                )
                recoveredJobs++
                activeJob = jobRepository.findActive(ACTIVE_STATUSES)
            }
            recoveredJobs
        }

    private companion object {
        val creationLock = ReentrantLock()
        val ACTIVE_STATUSES = setOf(BdiRefreshJobStatus.PENDING, BdiRefreshJobStatus.RUNNING)
        const val SCHEDULING_FAILED = "BDI_REFRESH_SCHEDULING_FAILED"
        const val SCHEDULING_FAILURE_MESSAGE = "The BDI refresh could not be submitted for execution"
        const val INTERRUPTED = "BDI_REFRESH_INTERRUPTED"
        const val INTERRUPTION_MESSAGE = "The BDI refresh was interrupted by an application restart"
    }
}
