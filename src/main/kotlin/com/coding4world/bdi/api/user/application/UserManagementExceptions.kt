package com.coding4world.bdi.api.user.application

class UserAlreadyExistsException : RuntimeException("A user with this email already exists")

class UserNotFoundException(
    val userId: String,
) : RuntimeException("User $userId was not found")

class InvalidUserUpdateException(message: String) : IllegalArgumentException(message)
