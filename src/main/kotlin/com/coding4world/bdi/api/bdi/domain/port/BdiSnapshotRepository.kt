package com.coding4world.bdi.api.bdi.domain.port

import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot
import com.coding4world.bdi.api.shared.domain.PageResult

interface BdiSnapshotRepository {
    fun save(snapshot: BdiSnapshot): BdiSnapshot

    fun findLatest(): BdiSnapshot?

    fun findByFingerprint(fingerprint: String): BdiSnapshot?

    fun findHistory(
        page: Int,
        size: Int,
    ): PageResult<BdiSnapshot>
}
