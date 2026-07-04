package com.coding4world.bdi.api.bdi.application

class BdiSourceRefreshException(
    cause: Exception,
) : RuntimeException("The external BDI source could not be refreshed", cause)
