package ru.posttrack.warehouse

import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.StorageCellId

interface StorageCellRepository {
    fun findByShipmentId(shipmentId: ShipmentId): StorageCell?

    fun findByIdForUpdate(id: StorageCellId): StorageCell?

    fun findByShipmentIdForUpdate(shipmentId: ShipmentId): StorageCell?

    fun save(cell: StorageCell)
}
