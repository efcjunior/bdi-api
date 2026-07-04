package com.coding4world.bdi.api.shared.config

import com.coding4world.bdi.Coding4WorldBdiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ApplicationConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun coding4WorldBdiClient(): Coding4WorldBdiClient = Coding4WorldBdiClient()
}
