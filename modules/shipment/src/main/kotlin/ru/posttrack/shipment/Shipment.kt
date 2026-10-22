package ru.posttrack.shipment

import ru.posttrack.pricing.DeliveryQuote
import ru.posttrack.shared.EmailAddress
import ru.posttrack.shared.ParcelParameters
import ru.posttrack.shared.PhoneNumber
import ru.posttrack.shared.PostalAddress
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.TrackingNumber
import ru.posttrack.shared.UserId
import ru.posttrack.shared.WeightGrams
import java.time.Instant
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil

enum class ShipmentStatus {
    CREATED,
    ACCEPTED,
    IN_TRANSIT,
    READY_FOR_PICKUP,
    DELIVERED,
}

data class Recipient(
    val fullName: String,
    val phone: PhoneNumber,
    val email: EmailAddress?,
) {
    init {
        require(fullName.isNotBlank()) { "Recipient name must not be blank" }
        require(fullName.length <= 200) { "Recipient name must not exceed 200 characters" }
    }
}

class Shipment private constructor(
    val id: ShipmentId,
    val trackingNumber: TrackingNumber,
    val senderId: UserId,
    val recipient: Recipient,
    val from: PostalAddress,
    val to: PostalAddress,
    val parcel: ParcelParameters,
    val quote: DeliveryQuote,
    status: ShipmentStatus,
    actualWeight: WeightGrams?,
    expectedDeliveryDate: LocalDate,
    val createdAt: Instant,
    updatedAt: Instant,
) {
    var status: ShipmentStatus = status
        private set

    var actualWeight: WeightGrams? = actualWeight
        private set

    var expectedDeliveryDate: LocalDate = expectedDeliveryDate
        private set

    var updatedAt: Instant = updatedAt
        private set

    fun accept(
        weight: WeightGrams,
        at: Instant,
    ) {
        requireTransition(ShipmentStatus.ACCEPTED, at)
        val allowedDifference = maxOf(50, ceil(parcel.weight.value * 0.05).toInt())
        require(abs(parcel.weight.value - weight.value) <= allowedDifference) {
            "Actual weight differs from declared weight by more than $allowedDifference grams"
        }
        actualWeight = weight
        status = ShipmentStatus.ACCEPTED
        updatedAt = at
    }

    fun send(at: Instant) = changeStatus(ShipmentStatus.IN_TRANSIT, at)

    fun makeReadyForPickup(at: Instant) = changeStatus(ShipmentStatus.READY_FOR_PICKUP, at)

    fun deliver(at: Instant) = changeStatus(ShipmentStatus.DELIVERED, at)

    fun changeExpectedDelivery(
        date: LocalDate,
        at: Instant,
    ) {
        require(status == ShipmentStatus.ACCEPTED || status == ShipmentStatus.IN_TRANSIT)
        require(!at.isBefore(updatedAt))
        expectedDeliveryDate = date
        updatedAt = at
    }

    private fun changeStatus(
        newStatus: ShipmentStatus,
        at: Instant,
    ) {
        requireTransition(newStatus, at)
        status = newStatus
        updatedAt = at
    }

    private fun requireTransition(
        newStatus: ShipmentStatus,
        at: Instant,
    ) {
        require(NEXT_STATUS[status] == newStatus) { "Invalid transition from $status to $newStatus" }
        require(!at.isBefore(updatedAt)) { "Status time cannot move backwards" }
    }

    companion object {
        private val NEXT_STATUS =
            mapOf(
                ShipmentStatus.CREATED to ShipmentStatus.ACCEPTED,
                ShipmentStatus.ACCEPTED to ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.IN_TRANSIT to ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.READY_FOR_PICKUP to ShipmentStatus.DELIVERED,
            )

        fun create(
            id: ShipmentId,
            trackingNumber: TrackingNumber,
            senderId: UserId,
            recipient: Recipient,
            from: PostalAddress,
            to: PostalAddress,
            quote: DeliveryQuote,
            at: Instant,
        ): Shipment {
            validateQuote(quote, senderId, from, to)
            require(!quote.isExpired(at)) { "Quote has expired" }
            return Shipment(
                id,
                trackingNumber,
                senderId,
                recipient,
                from,
                to,
                quote.request.parcel,
                quote,
                ShipmentStatus.CREATED,
                null,
                quote.deliveryTo,
                at,
                at,
            )
        }

        fun restore(
            id: ShipmentId,
            trackingNumber: TrackingNumber,
            senderId: UserId,
            recipient: Recipient,
            from: PostalAddress,
            to: PostalAddress,
            quote: DeliveryQuote,
            status: ShipmentStatus,
            actualWeight: WeightGrams?,
            expectedDeliveryDate: LocalDate,
            createdAt: Instant,
            updatedAt: Instant,
        ): Shipment {
            validateQuote(quote, senderId, from, to)
            require(!updatedAt.isBefore(createdAt)) { "Shipment update time cannot precede creation" }
            require((status == ShipmentStatus.CREATED) == (actualWeight == null)) {
                "Actual weight must be set only after shipment acceptance"
            }
            return Shipment(
                id,
                trackingNumber,
                senderId,
                recipient,
                from,
                to,
                quote.request.parcel,
                quote,
                status,
                actualWeight,
                expectedDeliveryDate,
                createdAt,
                updatedAt,
            )
        }

        private fun validateQuote(
            quote: DeliveryQuote,
            senderId: UserId,
            from: PostalAddress,
            to: PostalAddress,
        ) {
            require(quote.userId == senderId) { "Quote belongs to another user" }
            require(quote.request.from == from.postalCode && quote.request.to == to.postalCode) {
                "Quote route differs from shipment route"
            }
        }
    }
}

fun interface TrackingNumberGenerator {
    fun generate(): TrackingNumber
}
