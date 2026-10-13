package ru.posttrack.identity

import ru.posttrack.shared.EmailAddress
import ru.posttrack.shared.UserId
import java.time.Instant

enum class UserRole {
    CLIENT,
    OPERATOR,
    COURIER,
    ADMIN,
}

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
}

data class User(
    val id: UserId,
    val email: EmailAddress,
    val passwordHash: String,
    val roles: Set<UserRole> = setOf(UserRole.CLIENT),
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Instant,
) {
    init {
        require(passwordHash.isNotBlank()) { "Password hash must not be blank" }
        require(passwordHash.length <= 255) { "Password hash must not exceed 255 characters" }
        require(roles.isNotEmpty()) { "User must have at least one role" }
    }
}
