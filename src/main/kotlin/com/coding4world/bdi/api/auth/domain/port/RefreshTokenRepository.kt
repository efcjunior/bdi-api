package com.coding4world.bdi.api.auth.domain.port

import com.coding4world.bdi.api.auth.domain.model.RefreshToken

interface RefreshTokenRepository {
    fun save(token: RefreshToken): RefreshToken

    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun findAllByFamilyId(familyId: String): List<RefreshToken>
}
