package ru.posttrack.postgres

import ru.posttrack.identity.User
import ru.posttrack.identity.UserRepository
import ru.posttrack.identity.UserRole
import ru.posttrack.identity.UserStatus
import ru.posttrack.shared.EmailAddress
import ru.posttrack.shared.UserId
import java.util.UUID

class PostgresUserRepository(
    private val database: PostgresDatabase,
) : UserRepository {
    override fun findById(id: UserId): User? {
        val sql = "SELECT * FROM posttrack.users WHERE id = ?"
        return database.connection().prepareStatement(sql).use { statement ->
            statement.setObject(1, id.value)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return null
                val rolesArray = rows.getArray("roles")
                val roles =
                    try {
                        (rolesArray.array as Array<*>).map { UserRole.valueOf(it as String) }.toSet()
                    } finally {
                        rolesArray.free()
                    }
                User(
                    id = UserId(rows.getObject("id", UUID::class.java)),
                    email = EmailAddress.of(rows.getString("email")),
                    passwordHash = rows.getString("password_hash"),
                    roles = roles,
                    status = UserStatus.valueOf(rows.getString("status")),
                    createdAt = rows.getTimestamp("created_at").toInstant(),
                )
            }
        }
    }
}
