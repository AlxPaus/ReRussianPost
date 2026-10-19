package ru.posttrack.identity

import ru.posttrack.shared.UserId

interface UserRepository {
    fun findById(id: UserId): User?
}
