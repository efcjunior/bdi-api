package com.coding4world.bdi.api.bdi.application

class BdiRefreshJobNotFoundException(
    val jobId: String,
) : RuntimeException("BDI refresh job $jobId was not found")
