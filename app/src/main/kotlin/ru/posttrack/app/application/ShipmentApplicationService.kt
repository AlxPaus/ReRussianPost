package ru.posttrack.app.application

import ru.posttrack.identity.UserRole
import ru.posttrack.pricing.DeliveryQuoteRepository
import ru.posttrack.shared.PostalAddress
import ru.posttrack.shared.QuoteId
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.TrackingEventId
import ru.posttrack.shared.TransactionManager
import ru.posttrack.shared.UserId
import ru.posttrack.shared.WeightGrams
import ru.posttrack.shipment.Recipient
import ru.posttrack.shipment.Shipment
import ru.posttrack.shipment.ShipmentRepository
import ru.posttrack.shipment.TrackingNumberGenerator
import ru.posttrack.tracking.TrackingEvent
import ru.posttrack.tracking.TrackingEventRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

data class CreateShipmentCommand(
    val quoteId: QuoteId,
    val recipient: Recipient,
    val from: PostalAddress,
    val to: PostalAddress,
)

class ShipmentApplicationService(
    private val shipments: ShipmentRepository,
    private val quotes: DeliveryQuoteRepository,
    private val events: TrackingEventRepository,
    private val trackingNumbers: TrackingNumberGenerator,
    private val transactions: TransactionManager,
    private val access: AccessPolicy,
    private val clock: Clock,
) {
    fun create(
        userId: UserId,
        command: CreateShipmentCommand,
    ): Shipment =
        transactions.execute {
            access.requireRole(userId, UserRole.CLIENT)
            val quote = quotes.findById(command.quoteId) ?: throw EntityNotFound("Quote")
            if (quote.userId != userId) throw AccessDenied()
            val shipment =
                Shipment.create(
                    id = ShipmentId.random(),
                    trackingNumber = trackingNumbers.generate(),
                    senderId = userId,
                    recipient = command.recipient,
                    from = command.from,
                    to = command.to,
                    quote = quote,
                    at = clock.instant(),
                )
            saveWithEvent(shipment, command.from.postalCode.value)
            shipment
        }

    fun accept(
        userId: UserId,
        shipmentId: ShipmentId,
        weight: WeightGrams,
        location: String,
    ): Shipment =
        update(userId, shipmentId, UserRole.OPERATOR, location) { shipment, now ->
            shipment.accept(weight, now)
        }

    fun send(
        userId: UserId,
        shipmentId: ShipmentId,
        location: String,
    ): Shipment =
        update(userId, shipmentId, UserRole.COURIER, location) { shipment, now ->
            shipment.send(now)
        }

    fun changeExpectedDelivery(
        userId: UserId,
        shipmentId: ShipmentId,
        date: LocalDate,
        location: String,
    ): Shipment =
        update(userId, shipmentId, UserRole.COURIER, location) { shipment, now ->
            require(!date.isBefore(LocalDate.now(clock))) { "Expected delivery date cannot be in the past" }
            shipment.changeExpectedDelivery(date, now)
        }

    private fun update(
        userId: UserId,
        shipmentId: ShipmentId,
        role: UserRole,
        location: String,
        action: (Shipment, Instant) -> Unit,
    ): Shipment =
        transactions.execute {
            access.requireRole(userId, role)
            val shipment = shipments.findByIdForUpdate(shipmentId) ?: throw EntityNotFound("Shipment")
            action(shipment, clock.instant())
            saveWithEvent(shipment, location)
            shipment
        }

    private fun saveWithEvent(
        shipment: Shipment,
        location: String,
    ) {
        val event = TrackingEvent.record(TrackingEventId.random(), shipment, location)
        shipments.save(shipment)
        events.save(event)
    }
}
