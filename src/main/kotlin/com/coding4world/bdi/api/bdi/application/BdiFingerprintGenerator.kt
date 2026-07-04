package com.coding4world.bdi.api.bdi.application

import com.coding4world.bdi.api.bdi.domain.model.BdiPublication
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Component
class BdiFingerprintGenerator {
    fun generate(publication: BdiPublication): String {
        val canonicalValue = publication.value.stripTrailingZeros().toPlainString()
        val canonicalContent =
            listOf(
                FINGERPRINT_VERSION,
                canonicalValue,
                publication.validFrom.toString(),
                publication.sourcePdf.normalize().toASCIIString(),
            ).joinToString("\n")

        return MessageDigest
            .getInstance("SHA-256")
            .digest(canonicalContent.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private companion object {
        const val FINGERPRINT_VERSION = "bdi-publication-v1"
    }
}
