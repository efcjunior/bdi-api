package com.coding4world.bdi.api.shared.security

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.time.Clock

@Configuration
class JwtConfiguration {
    @Bean
    fun jwtDecoder(
        properties: BdiApiProperties,
        clock: Clock,
    ): JwtDecoder {
        require(properties.security.jwt.jwksUri.isNotBlank()) {
            "AUTH_JWKS_URI must be configured"
        }
        val decoder = NimbusJwtDecoder.withJwkSetUri(properties.security.jwt.jwksUri).build()
        val timestampValidator = JwtTimestampValidator().apply { setClock(clock) }
        val issuerValidator =
            JwtValidators.createDefaultWithValidators(
                timestampValidator,
                JwtIssuerValidator(properties.security.jwt.issuer),
            )
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
