package com.coding4world.bdi.api.auth.infrastructure.persistence

import com.coding4world.bdi.api.auth.domain.model.RefreshToken
import com.coding4world.bdi.api.auth.domain.port.RefreshTokenRepository
import com.coding4world.bdi.api.shared.persistence.toMongoPrecision
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

internal interface SpringDataRefreshTokenRepository : MongoRepository<RefreshTokenDocument, String> {
    fun findByTokenHash(tokenHash: String): RefreshTokenDocument?

    fun findAllByFamilyId(familyId: String): List<RefreshTokenDocument>
}

@Repository
internal class MongoRefreshTokenRepository(
    private val repository: SpringDataRefreshTokenRepository,
) : RefreshTokenRepository {
    override fun save(token: RefreshToken): RefreshToken = repository.save(token.toDocument()).toDomain()

    override fun findByTokenHash(tokenHash: String): RefreshToken? = repository.findByTokenHash(tokenHash)?.toDomain()

    override fun findAllByFamilyId(familyId: String): List<RefreshToken> =
        repository.findAllByFamilyId(familyId).map(RefreshTokenDocument::toDomain)
}

private fun RefreshToken.toDocument() =
    RefreshTokenDocument(
        id = id,
        tokenHash = tokenHash,
        familyId = familyId,
        userId = userId,
        expiresAt = expiresAt.toMongoPrecision(),
        revokedAt = revokedAt?.toMongoPrecision(),
        replacementTokenHash = replacementTokenHash,
        createdAt = createdAt?.toMongoPrecision(),
        updatedAt = updatedAt?.toMongoPrecision(),
    )

private fun RefreshTokenDocument.toDomain() =
    RefreshToken(
        id = id,
        tokenHash = tokenHash,
        familyId = familyId,
        userId = userId,
        expiresAt = expiresAt.toMongoPrecision(),
        revokedAt = revokedAt?.toMongoPrecision(),
        replacementTokenHash = replacementTokenHash,
        createdAt = createdAt?.toMongoPrecision(),
        updatedAt = updatedAt?.toMongoPrecision(),
    )
