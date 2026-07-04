package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshTrigger

interface BdiRefreshJobs {
    fun request(
        trigger: BdiRefreshTrigger,
        requestedBy: String? = null,
    ): BdiRefreshJob

    fun findById(jobId: String): BdiRefreshJob?

    fun recoverOrphanedJobs(): Int
}
