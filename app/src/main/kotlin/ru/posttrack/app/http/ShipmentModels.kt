package ru.posttrack.app.http

import kotlinx.serialization.Serializable
import ru.posttrack.app.application.CreateShipmentCommand
import ru.posttrack.shared.EmailAddress
import ru.posttrack.shared.PhoneNumber
import ru.posttrack.shared.QuoteId
import ru.posttrack.shipment.Recipient
import ru.posttrack.shipment.Shipment
import ru.posttrack.tracking.TrackingEvent

@Serializable
data class RecipientDto(
    val fullName: String,
    val phone: String,
    val email: String? = null,
) {
    fun toDomain(): Recipient = Recipient(fullName, PhoneNumber.of(phone), email?.let(EmailAddress::of))
}

@Serializable
data class CreateShipmentRequest(
    val quoteId: String,
    val recipient: RecipientDto,
    val from: AddressDto,
    val to: AddressDto,
) {
    fun toCommand(): CreateShipmentCommand =
        CreateShipmentCommand(QuoteId(parseUuid(quoteId)), recipient.toDomain(), from.toDomain(), to.toDomain())
}

@Serializable
data class AcceptShipmentRequest(
    val weightGrams: Int,
    val location: String,
)

@Serializable
data class SendShipmentRequest(
    val location: String,
)

@Serializable
data class ChangeDeliveryRequest(
    val date: String,
    val location: String,
)

@Serializable
data class ShipmentResponse(
    val id: String,
    val trackingNumber: String,
    val senderId: String,
    val recipient: RecipientDto,
    val from: AddressDto,
    val to: AddressDto,
    val quote: QuoteResponse,
    val status: String,
    val actualWeightGrams: Int?,
    val expectedDeliveryDate: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class TrackingEventResponse(
    val id: String,
    val status: String,
    val location: String,
    val expectedDeliveryDate: String?,
    val occurredAt: String,
)

fun Shipment.toResponse(): ShipmentResponse =
    ShipmentResponse(
        id = id.value.toString(),
        trackingNumber = trackingNumber.value,
        senderId = senderId.value.toString(),
        recipient = RecipientDto(recipient.fullName, recipient.phone.value, recipient.email?.value),
        from = from.toDto(),
        to = to.toDto(),
        quote = quote.toResponse(),
        status = status.name,
        actualWeightGrams = actualWeight?.value,
        expectedDeliveryDate = expectedDeliveryDate.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )

fun TrackingEvent.toResponse(): TrackingEventResponse =
    TrackingEventResponse(id.value.toString(), status.name, location, expectedDeliveryDate?.toString(), occurredAt.toString())
