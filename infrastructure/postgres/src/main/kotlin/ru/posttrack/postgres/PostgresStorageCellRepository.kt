package ru.posttrack.postgres

import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.StorageCellId
import ru.posttrack.warehouse.StorageCell
import ru.posttrack.warehouse.StorageCellRepository
import java.sql.ResultSet
import java.util.UUID

class PostgresStorageCellRepository(
    private val database: PostgresDatabase,
) : StorageCellRepository {
    override fun findByShipmentId(shipmentId: ShipmentId): StorageCell? = find("WHERE occupied_by_shipment_id = ?", shipmentId.value)

    override fun findByIdForUpdate(id: StorageCellId): StorageCell? = find("WHERE id = ? FOR UPDATE", id.value)

    override fun findByShipmentIdForUpdate(shipmentId: ShipmentId): StorageCell? =
        find("WHERE occupied_by_shipment_id = ? FOR UPDATE", shipmentId.value)

    private fun find(
        condition: String,
        id: UUID,
    ): StorageCell? {
        val sql = "SELECT * FROM posttrack.storage_cells $condition"
        return database.connection().prepareStatement(sql).use { statement ->
            statement.setObject(1, id)
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.toStorageCell() else null
            }
        }
    }

    override fun save(cell: StorageCell) {
        database.insert(
            table = "posttrack.storage_cells",
            values =
                mapOf(
                    "id" to cell.id.value,
                    "post_office_index" to cell.postOffice.value,
                    "rack" to cell.rack,
                    "shelf" to cell.shelf,
                    "cell" to cell.cell,
                    "occupied_by_shipment_id" to cell.occupiedBy?.value,
                ),
            updateColumns = listOf("occupied_by_shipment_id"),
        )
    }
}

private fun ResultSet.toStorageCell(): StorageCell =
    StorageCell.restore(
        id = StorageCellId(getObject("id", UUID::class.java)),
        postOffice = PostalCode(getString("post_office_index")),
        rack = getInt("rack"),
        shelf = getInt("shelf"),
        cell = getInt("cell"),
        occupiedBy = getObject("occupied_by_shipment_id", UUID::class.java)?.let(::ShipmentId),
    )
