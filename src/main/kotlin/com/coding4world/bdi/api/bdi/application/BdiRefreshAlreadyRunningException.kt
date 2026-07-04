package com.coding4world.bdi.api.bdi.application

class BdiRefreshAlreadyRunningException(
    val activeJobId: String?,
) : IllegalStateException("A BDI refresh job is already active")
