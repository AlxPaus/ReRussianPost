package ru.posttrack.warehouse

import ru.posttrack.shared.ShipmentId

interface PickupCodeRepository {
    fun findByShipmentIdForUpdate(shipmentId: ShipmentId): PickupCode?

    fun save(code: PickupCode)
}
