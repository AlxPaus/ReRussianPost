package ru.posttrack.app.application

import ru.posttrack.identity.UserRole
import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.StorageCellId
import ru.posttrack.shared.TrackingEventId
import ru.posttrack.shared.TransactionManager
import ru.posttrack.shared.UserId
import ru.posttrack.shipment.Shipment
import ru.posttrack.shipment.ShipmentRepository
import ru.posttrack.tracking.TrackingEvent
import ru.posttrack.tracking.TrackingEventRepository
import ru.posttrack.warehouse.PickupCode
import ru.posttrack.warehouse.PickupCodeGenerator
import ru.posttrack.warehouse.PickupCodeHasher
import ru.posttrack.warehouse.PickupCodeRepository
import ru.posttrack.warehouse.PickupService
import ru.posttrack.warehouse.StorageCell
import ru.posttrack.warehouse.StorageCellRepository
import ru.posttrack.warehouse.WarehouseService
import java.time.Clock
import java.time.Duration

class WarehouseApplicationService(
    private val shipments: ShipmentRepository,
    private val cells: StorageCellRepository,
    private val codes: PickupCodeRepository,
    private val events: TrackingEventRepository,
    private val codeGenerator: PickupCodeGenerator,
    private val codeHasher: PickupCodeHasher,
    private val transactions: TransactionManager,
    private val access: AccessPolicy,
    private val clock: Clock,
    private val codeLifetime: Duration,
) {
    private val warehouse = WarehouseService()
    private val pickup = PickupService(codeHasher)

    init {
        require(!codeLifetime.isNegative && !codeLifetime.isZero) { "Pickup code lifetime must be positive" }
    }

    fun createCell(
        userId: UserId,
        postOffice: PostalCode,
        rack: Int,
        shelf: Int,
        cell: Int,
    ): StorageCell =
        transactions.execute {
            access.requireRole(userId, UserRole.OPERATOR)
            val storageCell = StorageCell.create(StorageCellId.random(), postOffice, rack, shelf, cell)
            cells.save(storageCell)
            storageCell
        }

    fun findCell(
        userId: UserId,
        shipmentId: ShipmentId,
    ): StorageCell =
        transactions.execute {
            access.requireRole(userId, UserRole.OPERATOR)
            cells.findByShipmentId(shipmentId) ?: throw EntityNotFound("Storage cell")
        }

    fun place(
        userId: UserId,
        shipmentId: ShipmentId,
        cellId: StorageCellId,
    ): Shipment =
        transactions.execute {
            access.requireRole(userId, UserRole.OPERATOR)
            val shipment = shipments.findByIdForUpdate(shipmentId) ?: throw EntityNotFound("Shipment")
            val cell = cells.findByIdForUpdate(cellId) ?: throw EntityNotFound("Storage cell")
            warehouse.place(shipment, cell, clock.instant())
            shipments.save(shipment)
            cells.save(cell)
            events.save(TrackingEvent.record(TrackingEventId.random(), shipment, cell.postOffice.value))
            shipment
        }

    fun issuePickupCode(
        userId: UserId,
        shipmentId: ShipmentId,
    ): String =
        transactions.execute {
            access.requireRole(userId, UserRole.OPERATOR)
            val shipment = shipments.findByIdForUpdate(shipmentId) ?: throw EntityNotFound("Shipment")
            val cell = cells.findByShipmentIdForUpdate(shipmentId) ?: throw EntityNotFound("Storage cell")
            require(cell.isOccupiedBy(shipmentId) && cell.postOffice == shipment.to.postalCode)
            val now = clock.instant()
            val issued = PickupCode.issue(shipment, codeGenerator, codeHasher, now, now.plus(codeLifetime))
            codes.save(issued.pickupCode)
            issued.code
        }

    fun deliver(
        userId: UserId,
        shipmentId: ShipmentId,
        code: String,
    ): Boolean =
        transactions.execute {
            access.requireRole(userId, UserRole.OPERATOR)
            val shipment = shipments.findByIdForUpdate(shipmentId) ?: throw EntityNotFound("Shipment")
            val cell = cells.findByShipmentIdForUpdate(shipmentId) ?: throw EntityNotFound("Storage cell")
            val pickupCode = codes.findByShipmentIdForUpdate(shipmentId) ?: throw EntityNotFound("Pickup code")
            val delivered = pickup.complete(shipment, cell, pickupCode, code, clock.instant())
            codes.save(pickupCode)
            if (delivered) {
                shipments.save(shipment)
                cells.save(cell)
                events.save(TrackingEvent.record(TrackingEventId.random(), shipment, cell.postOffice.value))
            }
            delivered
        }
}
