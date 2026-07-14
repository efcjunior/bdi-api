package com.coding4world.bdi.api.user.infrastructure.web

import com.coding4world.bdi.api.user.application.UserManagementService
import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.model.UserRole
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

@RestController
@RequestMapping("/api/v1/admin/users")
class UserManagementController(
    private val userManagementService: UserManagementService,
) {
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
    ): ResponseEntity<UserResponse> {
        val user =
            userManagementService.create(
                email = request.email,
                password = request.password,
                roles = request.roles,
                enabled = request.enabled,
            )
        return ResponseEntity
            .created(URI.create("/api/v1/admin/users/${user.id}"))
            .body(UserResponse.from(user))
    }

    @PatchMapping("/{userId}")
    fun update(
        @PathVariable userId: String,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserResponse = UserResponse.from(userManagementService.update(userId, request.roles, request.enabled))
}

data class CreateUserRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank @field:Size(min = 12, max = 128) val password: String,
    @field:NotEmpty val roles: Set<UserRole>,
    val enabled: Boolean = true,
)

data class UpdateUserRequest(
    val roles: Set<UserRole>? = null,
    val enabled: Boolean? = null,
)

data class UserResponse(
    val id: String,
    val email: String,
    val roles: Set<UserRole>,
    val enabled: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = requireNotNull(user.id),
                email = user.normalizedEmail,
                roles = user.roles,
                enabled = user.enabled,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
    }
}
