package ru.posttrack.shipment

import ru.posttrack.shared.PhoneNumber
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.TrackingNumber

interface ShipmentRepository {
    fun findByTrackingNumber(trackingNumber: TrackingNumber): Shipment?

    fun findByRecipientPhone(phone: PhoneNumber): List<Shipment>

    fun findByIdForUpdate(id: ShipmentId): Shipment?

    fun save(shipment: Shipment)
}
