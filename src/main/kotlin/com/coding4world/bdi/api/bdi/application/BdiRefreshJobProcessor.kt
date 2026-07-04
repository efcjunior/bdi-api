package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus
import com.coding4world.bdi.api.bdi.domain.port.BdiRefreshJobRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class BdiRefreshJobProcessor(
    private val jobRepository: BdiRefreshJobRepository,
    private val synchronizeCurrentBdi: SynchronizeCurrentBdi,
    private val clock: Clock,
) {
    fun process(jobId: String) {
        val pendingJob = jobRepository.findById(jobId) ?: return
        if (pendingJob.status != BdiRefreshJobStatus.PENDING) return

        val runningJob = jobRepository.save(
            pendingJob.copy(
                status = BdiRefreshJobStatus.RUNNING,
                startedAt = clock.instant(),
            ),
        )

        val result =
            try {
                synchronizeCurrentBdi.execute()
            } catch (exception: BdiSourceRefreshException) {
                failJob(runningJob, SOURCE_REFRESH_FAILED, SOURCE_FAILURE_MESSAGE, exception)
                return
            } catch (exception: Exception) {
                failJob(runningJob, REFRESH_PROCESSING_FAILED, PROCESSING_FAILURE_MESSAGE, exception)
                return
            }

        try {
            jobRepository.save(
                runningJob.copy(
                    status = BdiRefreshJobStatus.SUCCEEDED,
                    snapshotId = result.snapshot.id,
                    completedAt = clock.instant(),
                ),
            )
        } catch (exception: Exception) {
            failJob(runningJob, REFRESH_PERSISTENCE_FAILED, PERSISTENCE_FAILURE_MESSAGE, exception)
        }
    }

    private fun failJob(
        runningJob: BdiRefreshJob,
        errorCode: String,
        errorMessage: String,
        cause: Exception,
    ) {
        logger.warn(
            "BDI refresh job {} failed with code {} and exception type {}",
            runningJob.id,
            errorCode,
            cause.javaClass.simpleName,
        )
        try {
            jobRepository.save(
                runningJob.copy(
                    status = BdiRefreshJobStatus.FAILED,
                    completedAt = clock.instant(),
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                ),
            )
        } catch (persistenceException: Exception) {
            logger.error(
                "Could not persist the failure state for BDI refresh job {} due to {}",
                runningJob.id,
                persistenceException.javaClass.simpleName,
            )
        }
    }

    private companion object {
        const val SOURCE_REFRESH_FAILED = "BDI_SOURCE_REFRESH_FAILED"
        const val SOURCE_FAILURE_MESSAGE = "The external BDI source could not be refreshed"
        const val REFRESH_PROCESSING_FAILED = "BDI_REFRESH_PROCESSING_FAILED"
        const val PROCESSING_FAILURE_MESSAGE = "The BDI refresh could not be completed"
        const val REFRESH_PERSISTENCE_FAILED = "BDI_REFRESH_PERSISTENCE_FAILED"
        const val PERSISTENCE_FAILURE_MESSAGE = "The BDI refresh result could not be persisted"
        val logger = LoggerFactory.getLogger(BdiRefreshJobProcessor::class.java)
    }
}
