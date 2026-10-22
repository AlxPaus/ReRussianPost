package ru.posttrack.app.http

import kotlinx.serialization.Serializable
import ru.posttrack.warehouse.StorageCell

@Serializable
data class CreateCellRequest(
    val postOffice: String,
    val rack: Int,
    val shelf: Int,
    val cell: Int,
)

@Serializable
data class PlaceShipmentRequest(
    val cellId: String,
)

@Serializable
data class PickupCodeDto(
    val code: String,
)

@Serializable
data class DeliveryResponse(
    val delivered: Boolean,
)

@Serializable
data class StorageCellResponse(
    val id: String,
    val postOffice: String,
    val rack: Int,
    val shelf: Int,
    val cell: Int,
    val shipmentId: String?,
)

fun StorageCell.toResponse(): StorageCellResponse =
    StorageCellResponse(id.value.toString(), postOffice.value, rack, shelf, cell, occupiedBy?.value?.toString())
