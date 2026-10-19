package ru.posttrack.postgres

import ru.posttrack.shared.ShipmentId
import ru.posttrack.warehouse.PickupCode
import ru.posttrack.warehouse.PickupCodeRepository
import java.sql.ResultSet
import java.util.UUID

class PostgresPickupCodeRepository(
    private val database: PostgresDatabase,
) : PickupCodeRepository {
    override fun findByShipmentIdForUpdate(shipmentId: ShipmentId): PickupCode? {
        val sql = "SELECT * FROM posttrack.pickup_codes WHERE shipment_id = ? FOR UPDATE"
        return database.connection().prepareStatement(sql).use { statement ->
            statement.setObject(1, shipmentId.value)
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.toPickupCode() else null
            }
        }
    }

    override fun save(code: PickupCode) {
        database.insert(
            table = "posttrack.pickup_codes",
            values =
                mapOf(
                    "shipment_id" to code.shipmentId.value,
                    "code_hash" to code.hash,
                    "issued_at" to code.issuedAt,
                    "expires_at" to code.expiresAt,
                    "attempts_left" to code.attemptsLeft,
                    "used_at" to code.usedAt,
                ),
            updateColumns = listOf("code_hash", "issued_at", "expires_at", "attempts_left", "used_at"),
            conflictColumn = "shipment_id",
        )
    }
}

private fun ResultSet.toPickupCode(): PickupCode =
    PickupCode.restore(
        shipmentId = ShipmentId(getObject("shipment_id", UUID::class.java)),
        hash = getString("code_hash"),
        issuedAt = getTimestamp("issued_at").toInstant(),
        expiresAt = getTimestamp("expires_at").toInstant(),
        attemptsLeft = getInt("attempts_left"),
        usedAt = getTimestamp("used_at")?.toInstant(),
    )
