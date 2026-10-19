package ru.posttrack.app.application

import ru.posttrack.identity.User
import ru.posttrack.identity.UserRepository
import ru.posttrack.identity.UserRole
import ru.posttrack.identity.UserStatus
import ru.posttrack.shared.UserId
import ru.posttrack.shipment.Shipment

class AccessPolicy(
    private val users: UserRepository,
) {
    fun requireRole(
        userId: UserId,
        vararg roles: UserRole,
    ): User {
        val user = users.findById(userId) ?: throw AccessDenied()
        if (user.status != UserStatus.ACTIVE) throw AccessDenied()
        if (UserRole.ADMIN !in user.roles && roles.none { it in user.roles }) throw AccessDenied()
        return user
    }

    fun requireShipmentAccess(
        user: User,
        shipment: Shipment,
    ) {
        if (user.id == shipment.senderId) return
        if (user.roles.any { it == UserRole.OPERATOR || it == UserRole.COURIER || it == UserRole.ADMIN }) return
        throw AccessDenied()
    }
}
