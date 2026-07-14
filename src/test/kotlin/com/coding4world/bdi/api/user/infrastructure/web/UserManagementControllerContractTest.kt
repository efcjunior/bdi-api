package com.coding4world.bdi.api.user.infrastructure.web

import com.coding4world.bdi.api.shared.web.ApiExceptionHandler
import com.coding4world.bdi.api.shared.web.ApiProblemFactory
import com.coding4world.bdi.api.shared.web.TraceIdFilter
import com.coding4world.bdi.api.user.application.UserManagementService
import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.model.UserRole
import com.coding4world.bdi.api.user.domain.port.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserManagementControllerContractTest {
    private val repository = ContractUserRepository()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val controller = UserManagementController(UserManagementService(repository, ContractPasswordEncoder()))
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(ApiExceptionHandler(ApiProblemFactory()))
                .addFilters<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(TraceIdFilter())
                .build()
    }

    @Test
    fun `administrator creates a user without exposing password hash`() {
        mockMvc.perform(
            post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"email":"USER@EXAMPLE.COM","password":"strong-password","roles":["USER"]}""",
                ),
        ).andExpect(status().isCreated)
            .andExpect(header().string("Location", "/api/v1/admin/users/user-1"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.roles[0]").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    @Test
    fun `administrator can disable an existing user`() {
        repository.save(
            User(
                id = "user-existing",
                normalizedEmail = "user@example.com",
                passwordHash = "encoded",
                roles = setOf(UserRole.USER),
                enabled = true,
            ),
        )

        mockMvc.perform(
            patch("/api/v1/admin/users/user-existing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(false))
    }

    @Test
    fun `short password returns validation problem details`() {
        mockMvc.perform(
            post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"short","roles":["USER"]}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.violations[0].field").value("password"))
    }

    @Test
    fun `empty patch returns invalid request problem`() {
        mockMvc.perform(
            patch("/api/v1/admin/users/user-existing")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }
}

private class ContractPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String? = rawPassword?.let { "encoded:$it" }

    override fun matches(
        rawPassword: CharSequence?,
        encodedPassword: String?,
    ): Boolean = rawPassword != null && encode(rawPassword) == encodedPassword
}

private class ContractUserRepository : UserRepository {
    private val users = linkedMapOf<String, User>()

    override fun save(user: User): User {
        val saved = user.copy(id = user.id ?: "user-${users.size + 1}")
        users[requireNotNull(saved.id)] = saved
        return saved
    }

    override fun findByNormalizedEmail(normalizedEmail: String): User? =
        users.values.firstOrNull { it.normalizedEmail == normalizedEmail }

    override fun findById(id: String): User? = users[id]

    override fun existsByRole(role: UserRole): Boolean = users.values.any { role in it.roles }
}
