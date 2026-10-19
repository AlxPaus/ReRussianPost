package ru.posttrack.postgres

import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.TrackingEventId
import ru.posttrack.shipment.ShipmentStatus
import ru.posttrack.tracking.TrackingEvent
import ru.posttrack.tracking.TrackingEventRepository
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class PostgresTrackingEventRepository(
    private val database: PostgresDatabase,
) : TrackingEventRepository {
    override fun findByShipmentId(shipmentId: ShipmentId): List<TrackingEvent> {
        val sql = "SELECT * FROM posttrack.tracking_events WHERE shipment_id = ? ORDER BY occurred_at, id"
        return database.connection().prepareStatement(sql).use { statement ->
            statement.setObject(1, shipmentId.value)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(rows.toTrackingEvent())
                }
            }
        }
    }

    override fun save(event: TrackingEvent) {
        database.insert(
            table = "posttrack.tracking_events",
            values =
                mapOf(
                    "id" to event.id.value,
                    "shipment_id" to event.shipmentId.value,
                    "status" to event.status.name,
                    "location" to event.location,
                    "expected_delivery_date" to event.expectedDeliveryDate,
                    "occurred_at" to event.occurredAt,
                ),
        )
    }
}

private fun ResultSet.toTrackingEvent(): TrackingEvent =
    TrackingEvent.restore(
        id = TrackingEventId(getObject("id", UUID::class.java)),
        shipmentId = ShipmentId(getObject("shipment_id", UUID::class.java)),
        status = ShipmentStatus.valueOf(getString("status")),
        location = getString("location"),
        expectedDeliveryDate = getObject("expected_delivery_date", LocalDate::class.java),
        occurredAt = getTimestamp("occurred_at").toInstant(),
    )
