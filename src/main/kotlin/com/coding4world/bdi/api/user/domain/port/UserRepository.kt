package com.coding4world.bdi.api.user.domain.port

import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.model.UserRole

interface UserRepository {
    fun save(user: User): User

    fun findByNormalizedEmail(normalizedEmail: String): User?

    fun findById(id: String): User?

    fun existsByRole(role: UserRole): Boolean
}
