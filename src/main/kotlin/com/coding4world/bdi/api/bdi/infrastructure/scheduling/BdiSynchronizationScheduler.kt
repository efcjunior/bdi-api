package com.coding4world.bdi.api.bdi.infrastructure.scheduling

import com.coding4world.bdi.api.bdi.application.BdiRefreshAlreadyRunningException
import com.coding4world.bdi.api.bdi.application.BdiRefreshJobs
import com.coding4world.bdi.api.bdi.application.BdiRefreshSchedulingException
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "bdi-api.synchronization",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
internal class BdiSynchronizationScheduler(
    private val refreshJobs: BdiRefreshJobs,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun refreshAtStartup() {
        val recoveredJobs = refreshJobs.recoverOrphanedJobs()
        if (recoveredJobs > 0) {
            logger.warn("Recovered {} interrupted BDI refresh job(s) at startup", recoveredJobs)
        }
        requestRefresh(BdiRefreshTrigger.STARTUP)
    }

    @Scheduled(
        fixedDelayString = "\${bdi-api.synchronization.interval:PT6H}",
        initialDelayString = "\${bdi-api.synchronization.interval:PT6H}",
    )
    fun refreshOnSchedule() {
        requestRefresh(BdiRefreshTrigger.SCHEDULED)
    }

    private fun requestRefresh(trigger: BdiRefreshTrigger) {
        try {
            val job = refreshJobs.request(trigger)
            logger.info("Created BDI refresh job {} with trigger {}", job.id, trigger)
        } catch (exception: BdiRefreshAlreadyRunningException) {
            logger.info("Skipped {} BDI refresh because job {} is still active", trigger, exception.activeJobId)
        } catch (exception: BdiRefreshSchedulingException) {
            logger.error("Could not submit BDI refresh job {} for execution", exception.jobId)
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(BdiSynchronizationScheduler::class.java)
    }
}
