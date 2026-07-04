package com.coding4world.bdi.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BdiApiApplication

fun main(args: Array<String>) {
    runApplication<BdiApiApplication>(*args)
}

