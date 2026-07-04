package com.coding4world.bdi.api.auth.infrastructure.security

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder

@Configuration
class JwtConfiguration {
    @Bean
    fun jwtEncoder(keys: RsaKeyMaterial): JwtEncoder {
        val rsaKey = RSAKey.Builder(keys.publicKey).privateKey(keys.privateKey).build()
        return NimbusJwtEncoder(ImmutableJWKSet<SecurityContext>(JWKSet(rsaKey)))
    }

    @Bean
    fun jwtDecoder(
        keys: RsaKeyMaterial,
        properties: BdiApiProperties,
    ): JwtDecoder {
        val decoder =
            NimbusJwtDecoder
                .withPublicKey(keys.publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build()
        val issuerValidator = JwtValidators.createDefaultWithIssuer(properties.security.jwt.issuer)
        val audienceValidator = audienceValidator(properties.security.jwt.audience)
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(issuerValidator, audienceValidator))
        return decoder
    }

    private fun audienceValidator(requiredAudience: String): OAuth2TokenValidator<Jwt> =
        OAuth2TokenValidator { jwt ->
            if (requiredAudience in jwt.audience.orEmpty()) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "The required audience is missing", null),
                )
            }
        }
}
