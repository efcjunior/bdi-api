package com.coding4world.bdi.api.auth.infrastructure.security

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class RsaKeyConfigurationTest {
    private val configuration = RsaKeyConfiguration()

    @Test
    fun `production key material requires configured public and private keys`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                configuration.configuredRsaKeyMaterial(BdiApiProperties())
            }

        assertThat(exception.message).isEqualTo("JWT_PUBLIC_KEY and JWT_PRIVATE_KEY must be configured outside local and test profiles")
    }

    @Test
    fun `local and test profiles use ephemeral RSA key material`() {
        val keys = configuration.ephemeralRsaKeyMaterial()

        assertThat(keys.publicKey).isInstanceOf(RSAPublicKey::class.java)
        assertThat(keys.privateKey).isInstanceOf(RSAPrivateKey::class.java)
        assertThat(keys.publicKey.modulus.bitLength()).isGreaterThanOrEqualTo(2048)
    }
}
