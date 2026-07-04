package com.coding4world.bdi.api.bdi.application

class BdiRefreshSchedulingException(
    val jobId: String,
    cause: Exception,
) : IllegalStateException("BDI refresh job $jobId could not be submitted for execution", cause)
