package com.coding4world.bdi.api.user.application

import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.model.UserRole
import com.coding4world.bdi.api.user.domain.port.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class UserManagementService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun create(
        email: String,
        password: String,
        roles: Set<UserRole>,
        enabled: Boolean,
    ): User {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        if (userRepository.findByNormalizedEmail(normalizedEmail) != null) {
            throw UserAlreadyExistsException()
        }
        if (roles.isEmpty()) throw InvalidUserUpdateException("At least one role is required")

        return userRepository.save(
            User(
                normalizedEmail = normalizedEmail,
                passwordHash = requireNotNull(passwordEncoder.encode(password)),
                roles = roles,
                enabled = enabled,
            ),
        )
    }

    fun update(
        userId: String,
        roles: Set<UserRole>?,
        enabled: Boolean?,
    ): User {
        if (roles == null && enabled == null) {
            throw InvalidUserUpdateException("At least one field must be supplied")
        }
        if (roles?.isEmpty() == true) throw InvalidUserUpdateException("At least one role is required")
        val existing = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return userRepository.save(
            existing.copy(
                roles = roles ?: existing.roles,
                enabled = enabled ?: existing.enabled,
            ),
        )
    }
}
