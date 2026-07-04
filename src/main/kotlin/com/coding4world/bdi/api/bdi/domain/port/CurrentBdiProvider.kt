package com.coding4world.bdi.api.bdi.domain.port

import com.coding4world.bdi.api.bdi.domain.model.BdiPublication

/** Supplies the BDI publication currently exposed by the external source. */
fun interface CurrentBdiProvider {
    fun current(): BdiPublication
}
