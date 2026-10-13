package ru.posttrack.warehouse

import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.StorageCellId
import ru.posttrack.shipment.Shipment
import ru.posttrack.shipment.ShipmentStatus
import java.time.Instant

class StorageCell private constructor(
    val id: StorageCellId,
    val postOffice: PostalCode,
    val rack: Int,
    val shelf: Int,
    val cell: Int,
    occupiedBy: ShipmentId? = null,
) {
    var occupiedBy: ShipmentId? = occupiedBy
        private set

    init {
        require(rack > 0 && shelf > 0 && cell > 0)
    }

    fun occupy(shipment: Shipment) {
        require(shipment.status == ShipmentStatus.READY_FOR_PICKUP) { "Shipment is not ready for pickup" }
        require(shipment.to.postalCode == postOffice) { "Storage cell belongs to another post office" }
        require(occupiedBy == null || occupiedBy == shipment.id) { "Storage cell is occupied" }
        occupiedBy = shipment.id
    }

    fun release(shipmentId: ShipmentId) {
        require(occupiedBy == shipmentId) { "Storage cell belongs to another shipment" }
        occupiedBy = null
    }

    fun isOccupiedBy(shipmentId: ShipmentId): Boolean = occupiedBy == shipmentId

    companion object {
        fun create(
            id: StorageCellId,
            postOffice: PostalCode,
            rack: Int,
            shelf: Int,
            cell: Int,
        ): StorageCell = StorageCell(id, postOffice, rack, shelf, cell)

        fun restore(
            id: StorageCellId,
            postOffice: PostalCode,
            rack: Int,
            shelf: Int,
            cell: Int,
            occupiedBy: ShipmentId?,
        ): StorageCell = StorageCell(id, postOffice, rack, shelf, cell, occupiedBy)
    }
}

class PickupCode private constructor(
    val shipmentId: ShipmentId,
    val hash: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    attemptsLeft: Int = 5,
    usedAt: Instant? = null,
) {
    var attemptsLeft: Int = attemptsLeft
        private set

    var usedAt: Instant? = usedAt
        private set

    init {
        require(hash.isNotBlank() && hash.length <= 255) { "Invalid pickup code hash" }
        require(expiresAt.isAfter(issuedAt)) { "Pickup code expiration must follow its issue time" }
        require(attemptsLeft in 0..5) { "Pickup code attempts must be between 0 and 5" }
        require(usedAt == null || (!usedAt.isBefore(issuedAt) && usedAt.isBefore(expiresAt))) {
            "Pickup code must be used between issue and expiration"
        }
    }

    fun verify(
        code: String,
        hasher: PickupCodeHasher,
        at: Instant,
    ): Boolean {
        if (usedAt != null || at.isBefore(issuedAt) || !at.isBefore(expiresAt) || attemptsLeft == 0) return false
        if (!hasher.matches(code, hash)) {
            attemptsLeft--
            return false
        }
        usedAt = at
        return true
    }

    companion object {
        private val CODE_PATTERN = Regex("\\d{6}")

        fun issue(
            shipment: Shipment,
            generator: PickupCodeGenerator,
            hasher: PickupCodeHasher,
            issuedAt: Instant,
            expiresAt: Instant,
        ): IssuedPickupCode {
            require(shipment.status == ShipmentStatus.READY_FOR_PICKUP) { "Shipment is not ready for pickup" }
            val code = generator.generate().trim()
            require(code.matches(CODE_PATTERN)) { "Pickup code must contain exactly 6 digits" }
            val pickupCode =
                PickupCode(
                    shipmentId = shipment.id,
                    hash = hasher.hash(code),
                    issuedAt = issuedAt,
                    expiresAt = expiresAt,
                    attemptsLeft = 5,
                    usedAt = null,
                )
            return IssuedPickupCode(code, pickupCode)
        }

        fun restore(
            shipmentId: ShipmentId,
            hash: String,
            issuedAt: Instant,
            expiresAt: Instant,
            attemptsLeft: Int,
            usedAt: Instant?,
        ): PickupCode = PickupCode(shipmentId, hash, issuedAt, expiresAt, attemptsLeft, usedAt)
    }
}

data class IssuedPickupCode(
    val code: String,
    val pickupCode: PickupCode,
)

fun interface PickupCodeGenerator {
    fun generate(): String
}

interface PickupCodeHasher {
    fun hash(code: String): String

    fun matches(
        code: String,
        hash: String,
    ): Boolean
}

class WarehouseService {
    fun place(
        shipment: Shipment,
        storageCell: StorageCell,
        at: Instant,
    ) {
        require(shipment.status == ShipmentStatus.IN_TRANSIT) { "Shipment is not in transit" }
        require(storageCell.postOffice == shipment.to.postalCode) { "Storage cell belongs to another post office" }
        require(storageCell.occupiedBy == null) { "Storage cell is occupied" }
        require(!at.isBefore(shipment.updatedAt)) { "Placement time cannot precede shipment update" }
        shipment.makeReadyForPickup(at)
        storageCell.occupy(shipment)
    }
}

class PickupService(
    private val hasher: PickupCodeHasher,
) {
    fun complete(
        shipment: Shipment,
        storageCell: StorageCell,
        pickupCode: PickupCode,
        code: String,
        at: Instant,
    ): Boolean {
        require(shipment.status == ShipmentStatus.READY_FOR_PICKUP) { "Shipment is not ready for pickup" }
        require(storageCell.postOffice == shipment.to.postalCode) { "Storage cell belongs to another post office" }
        require(storageCell.isOccupiedBy(shipment.id)) { "Shipment is not stored in this cell" }
        require(pickupCode.shipmentId == shipment.id) { "Pickup code belongs to another shipment" }
        require(!at.isBefore(shipment.updatedAt)) { "Pickup time cannot precede shipment update" }
        if (!pickupCode.verify(code, hasher, at)) return false
        shipment.deliver(at)
        storageCell.release(shipment.id)
        return true
    }
}
