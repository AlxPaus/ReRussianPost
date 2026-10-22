package ru.posttrack.tracking

import ru.posttrack.shared.ShipmentId

interface TrackingEventRepository {
    fun findByShipmentId(shipmentId: ShipmentId): List<TrackingEvent>

    fun save(event: TrackingEvent)
}
