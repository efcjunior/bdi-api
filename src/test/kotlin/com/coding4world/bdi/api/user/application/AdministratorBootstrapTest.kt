package com.coding4world.bdi.api.user.application

import com.coding4world.bdi.api.shared.config.BdiApiProperties
import com.coding4world.bdi.api.user.domain.model.User
import com.coding4world.bdi.api.user.domain.model.UserRole
import com.coding4world.bdi.api.user.domain.port.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class AdministratorBootstrapTest {
    @Test
    fun `configured administrator is created only once`() {
        val repository = BootstrapUserRepository()
        val encoder = BootstrapPasswordEncoder()
        val properties =
            BdiApiProperties(
                security =
                    BdiApiProperties.Security(
                        bootstrapAdmin =
                            BdiApiProperties.BootstrapAdmin(
                                email = " ADMIN@EXAMPLE.COM ",
                                password = "strong-password",
                            ),
                    ),
            )
        val bootstrap = AdministratorBootstrap(repository, encoder, properties)

        bootstrap.createInitialAdministrator()
        bootstrap.createInitialAdministrator()

        assertThat(repository.users).hasSize(1)
        assertThat(repository.users.single().normalizedEmail).isEqualTo("admin@example.com")
        assertThat(repository.users.single().roles).containsExactly(UserRole.ADMIN)
        assertThat(repository.users.single().passwordHash).isEqualTo("encoded:strong-password")
    }

    @Test
    fun `missing bootstrap secrets leave users unchanged`() {
        val repository = BootstrapUserRepository()

        AdministratorBootstrap(repository, BootstrapPasswordEncoder(), BdiApiProperties())
            .createInitialAdministrator()

        assertThat(repository.users).isEmpty()
    }
}

private class BootstrapPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String? = rawPassword?.let { "encoded:$it" }

    override fun matches(
        rawPassword: CharSequence?,
        encodedPassword: String?,
    ): Boolean = rawPassword != null && encode(rawPassword) == encodedPassword
}

private class BootstrapUserRepository : UserRepository {
    val users = mutableListOf<User>()

    override fun save(user: User): User {
        val saved = user.copy(id = user.id ?: "user-${users.size + 1}")
        users.removeIf { it.id == saved.id }
        users += saved
        return saved
    }

    override fun findByNormalizedEmail(normalizedEmail: String): User? =
        users.firstOrNull { it.normalizedEmail == normalizedEmail }

    override fun findById(id: String): User? = users.firstOrNull { it.id == id }

    override fun existsByRole(role: UserRole): Boolean = users.any { role in it.roles }
}
