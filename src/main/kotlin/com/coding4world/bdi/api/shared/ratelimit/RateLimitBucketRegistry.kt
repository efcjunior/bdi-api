package com.coding4world.bdi.api.shared.ratelimit

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.Refill
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitBucketRegistry {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun tryConsume(
        policy: RateLimitPolicy,
        identity: String,
        configuration: BdiApiProperties.RateLimit.Policy,
    ): ConsumptionProbe =
        buckets
            .computeIfAbsent("$policy:$identity") { newBucket(configuration) }
            .tryConsumeAndReturnRemaining(1)

    private fun newBucket(configuration: BdiApiProperties.RateLimit.Policy): Bucket =
        Bucket
            .builder()
            .addLimit(Bandwidth.classic(configuration.capacity, Refill.intervally(configuration.capacity, configuration.refillPeriod)))
            .build()
}
