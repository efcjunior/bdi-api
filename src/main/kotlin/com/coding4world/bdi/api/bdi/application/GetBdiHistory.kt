package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.bdi.domain.port.BdiSnapshotRepository
import com.coding4world.bdi.api.shared.domain.PageResult
import org.springframework.stereotype.Service

@Service
class GetBdiHistory(
    private val snapshotRepository: BdiSnapshotRepository,
) {
    fun execute(
        page: Int,
        size: Int,
    ): PageResult<BdiSnapshot> {
        require(page >= 0) { "Page index must not be negative" }
        require(size in 1..MAXIMUM_PAGE_SIZE) { "Page size must be between 1 and $MAXIMUM_PAGE_SIZE" }
        return snapshotRepository.findHistory(page, size)
    }

    private companion object {
        const val MAXIMUM_PAGE_SIZE = 100
    }
}
