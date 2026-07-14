package com.coding4world.bdi.api.auth.application

interface AuthenticationOperations {
    fun login(
        email: String,
        password: String,
    ): AuthenticationTokens

    fun refresh(rawRefreshToken: String): AuthenticationTokens

    fun logout(rawRefreshToken: String)
}
