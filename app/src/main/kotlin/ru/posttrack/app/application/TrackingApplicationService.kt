package ru.posttrack.app.application

import ru.posttrack.identity.UserRole
import ru.posttrack.shared.PhoneNumber
import ru.posttrack.shared.TrackingNumber
import ru.posttrack.shared.TransactionManager
import ru.posttrack.shared.UserId
import ru.posttrack.shipment.Shipment
import ru.posttrack.shipment.ShipmentRepository
import ru.posttrack.tracking.TrackingEvent
import ru.posttrack.tracking.TrackingEventRepository

class TrackingApplicationService(
    private val shipments: ShipmentRepository,
    private val events: TrackingEventRepository,
    private val transactions: TransactionManager,
    private val access: AccessPolicy,
) {
    fun findShipment(
        userId: UserId,
        trackingNumber: TrackingNumber,
    ): Shipment =
        transactions.execute {
            findAccessibleShipment(userId, trackingNumber)
        }

    fun findByRecipientPhone(
        userId: UserId,
        phone: PhoneNumber,
    ): List<Shipment> =
        transactions.execute {
            access.requireRole(userId, UserRole.OPERATOR)
            shipments.findByRecipientPhone(phone)
        }

    fun history(
        userId: UserId,
        trackingNumber: TrackingNumber,
    ): List<TrackingEvent> =
        transactions.execute {
            val shipment = findAccessibleShipment(userId, trackingNumber)
            events.findByShipmentId(shipment.id).sortedBy { it.occurredAt }
        }

    private fun findAccessibleShipment(
        userId: UserId,
        trackingNumber: TrackingNumber,
    ): Shipment {
        val user = access.requireRole(userId, UserRole.CLIENT, UserRole.OPERATOR, UserRole.COURIER)
        val shipment = shipments.findByTrackingNumber(trackingNumber) ?: throw EntityNotFound("Shipment")
        access.requireShipmentAccess(user, shipment)
        return shipment
    }
}
