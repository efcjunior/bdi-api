package com.coding4world.bdi.api.bdi.domain.port

import com.coding4world.bdi.api.bdi.domain.model.BdiSnapshot

interface BdiSnapshotRepository {
    fun save(snapshot: BdiSnapshot): BdiSnapshot

    fun findLatest(): BdiSnapshot?

    fun findByFingerprint(fingerprint: String): BdiSnapshot?
}
