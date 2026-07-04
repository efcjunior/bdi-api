package com.coding4world.bdi.api.auth.application

import com.coding4world.bdi.api.auth.domain.model.RefreshToken
import com.coding4world.bdi.api.auth.domain.port.RefreshTokenRepository
import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.port.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.Locale
import java.util.UUID

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenService: JwtTokenService,
    private val properties: BdiApiProperties,
    private val clock: Clock,
) {
    private val secureRandom = SecureRandom()
    private val dummyPasswordHash = requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString()))

    fun login(
        email: String,
        password: String,
    ): AuthenticationTokens {
        val user = userRepository.findByNormalizedEmail(normalizeEmail(email))
        val passwordMatches = passwordEncoder.matches(password, user?.passwordHash ?: dummyPasswordHash)
        if (user == null || !user.enabled || !passwordMatches) {
            throw InvalidAuthenticationException()
        }
        return issueTokenPair(user, UUID.randomUUID().toString())
    }

    fun refresh(rawRefreshToken: String): AuthenticationTokens {
        val currentHash = hash(rawRefreshToken)
        val currentToken =
            refreshTokenRepository.findByTokenHash(currentHash)
                ?: throw InvalidAuthenticationException()
        val now = clock.instant()
        if (currentToken.revokedAt != null) {
            refreshTokenRepository.revokeFamily(currentToken.familyId, now)
            throw InvalidAuthenticationException()
        }
        if (!currentToken.expiresAt.isAfter(now)) {
            refreshTokenRepository.revokeIfActive(currentHash, now)
            throw InvalidAuthenticationException()
        }

        val user = userRepository.findById(currentToken.userId)
        if (user == null || !user.enabled) {
            refreshTokenRepository.revokeFamily(currentToken.familyId, now)
            throw InvalidAuthenticationException()
        }

        val replacement = createRefreshToken(user, currentToken.familyId)
        val savedReplacement = refreshTokenRepository.save(replacement.persisted)
        val consumed = refreshTokenRepository.revokeIfActive(currentHash, now, replacement.persisted.tokenHash)
        if (!consumed) {
            refreshTokenRepository.revokeFamily(currentToken.familyId, now)
            throw InvalidAuthenticationException()
        }

        return tokenPair(user, replacement.rawValue, savedReplacement)
    }

    fun logout(rawRefreshToken: String) {
        refreshTokenRepository.revokeIfActive(hash(rawRefreshToken), clock.instant())
    }

    private fun issueTokenPair(
        user: User,
        familyId: String,
    ): AuthenticationTokens {
        val refreshToken = createRefreshToken(user, familyId)
        val saved = refreshTokenRepository.save(refreshToken.persisted)
        return tokenPair(user, refreshToken.rawValue, saved)
    }

    private fun tokenPair(
        user: User,
        rawRefreshToken: String,
        persistedRefreshToken: RefreshToken,
    ): AuthenticationTokens {
        val accessToken = jwtTokenService.issue(user)
        return AuthenticationTokens(
            accessToken = accessToken.value,
            accessTokenExpiresAt = accessToken.expiresAt,
            refreshToken = rawRefreshToken,
            refreshTokenExpiresAt = persistedRefreshToken.expiresAt,
        )
    }

    private fun createRefreshToken(
        user: User,
        familyId: String,
    ): NewRefreshToken {
        val bytes = ByteArray(32).also(secureRandom::nextBytes)
        val rawValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return NewRefreshToken(
            rawValue = rawValue,
            persisted =
                RefreshToken(
                    tokenHash = hash(rawValue),
                    familyId = familyId,
                    userId = requireNotNull(user.id),
                    expiresAt = clock.instant().plus(properties.security.refreshTokenTtl),
                ),
        )
    }

    private fun hash(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)
}

data class AuthenticationTokens(
    val accessToken: String,
    val accessTokenExpiresAt: java.time.Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: java.time.Instant,
)

private data class NewRefreshToken(
    val rawValue: String,
    val persisted: RefreshToken,
)
