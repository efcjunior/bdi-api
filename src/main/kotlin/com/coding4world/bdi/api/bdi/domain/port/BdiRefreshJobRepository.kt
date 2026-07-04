package com.coding4world.bdi.api.bdi.domain.port

import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJob
import com.coding4world.bdi.api.bdi.domain.model.BdiRefreshJobStatus

interface BdiRefreshJobRepository {
    fun save(job: BdiRefreshJob): BdiRefreshJob

    fun findById(id: String): BdiRefreshJob?

    fun findActive(statuses: Set<BdiRefreshJobStatus>): BdiRefreshJob?
}
