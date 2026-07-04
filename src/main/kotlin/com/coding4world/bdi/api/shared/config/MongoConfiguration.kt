package com.coding4world.bdi.api.shared.config

import com.coding4world.bdi.api.auth.infrastructure.persistence.RefreshTokenDocument
import com.coding4world.bdi.api.bdi.infrastructure.persistence.BdiRefreshJobDocument
import com.coding4world.bdi.api.bdi.infrastructure.persistence.BdiSnapshotDocument
import com.coding4world.bdi.api.user.infrastructure.persistence.UserDocument
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component
import java.time.Duration

@Configuration
@EnableMongoAuditing
class MongoConfiguration

@Component
class MongoIndexInitializer(
    private val mongoTemplate: MongoTemplate,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun createIndexes() {
        mongoTemplate.indexOps(UserDocument::class.java).apply {
            createIndex(Index().on("normalizedEmail", Direction.ASC).unique().named("uk_users_normalized_email"))
        }

        mongoTemplate.indexOps(RefreshTokenDocument::class.java).apply {
            createIndex(Index().on("tokenHash", Direction.ASC).unique().named("uk_refresh_tokens_token_hash"))
            createIndex(Index().on("familyId", Direction.ASC).named("ix_refresh_tokens_family_id"))
            createIndex(Index().on("expiresAt", Direction.ASC).expire(Duration.ZERO).named("ttl_refresh_tokens_expires_at"))
        }

        mongoTemplate.indexOps(BdiSnapshotDocument::class.java).apply {
            createIndex(Index().on("fingerprint", Direction.ASC).unique().named("uk_bdi_snapshots_fingerprint"))
            createIndex(
                Index()
                    .on("validFrom", Direction.DESC)
                    .on("createdAt", Direction.DESC)
                    .named("ix_bdi_snapshots_latest"),
            )
        }

        mongoTemplate.indexOps(BdiRefreshJobDocument::class.java).apply {
            createIndex(Index().on("status", Direction.ASC).on("createdAt", Direction.ASC).named("ix_bdi_refresh_jobs_active"))
            createIndex(Index().on("expiresAt", Direction.ASC).expire(Duration.ZERO).named("ttl_bdi_refresh_jobs_expires_at"))
        }
    }
}
