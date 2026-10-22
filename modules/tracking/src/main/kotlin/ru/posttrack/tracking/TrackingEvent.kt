package ru.posttrack.tracking

import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.TrackingEventId
import ru.posttrack.shipment.Shipment
import ru.posttrack.shipment.ShipmentStatus
import java.time.Instant
import java.time.LocalDate

class TrackingEvent private constructor(
    val id: TrackingEventId,
    val shipmentId: ShipmentId,
    val status: ShipmentStatus,
    val location: String,
    val expectedDeliveryDate: LocalDate?,
    val occurredAt: Instant,
) {
    init {
        require(location.isNotBlank()) { "Location must not be blank" }
        require(location.length <= 255) { "Location must not exceed 255 characters" }
    }

    companion object {
        fun record(
            id: TrackingEventId,
            shipment: Shipment,
            location: String,
        ): TrackingEvent =
            TrackingEvent(
                id = id,
                shipmentId = shipment.id,
                status = shipment.status,
                location = location,
                expectedDeliveryDate = shipment.expectedDeliveryDate,
                occurredAt = shipment.updatedAt,
            )

        fun restore(
            id: TrackingEventId,
            shipmentId: ShipmentId,
            status: ShipmentStatus,
            location: String,
            expectedDeliveryDate: LocalDate?,
            occurredAt: Instant,
        ): TrackingEvent = TrackingEvent(id, shipmentId, status, location, expectedDeliveryDate, occurredAt)
    }
}
