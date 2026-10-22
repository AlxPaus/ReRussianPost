package ru.posttrack.postgres

import ru.posttrack.shared.EmailAddress
import ru.posttrack.shared.PhoneNumber
import ru.posttrack.shared.PostalAddress
import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.ShipmentId
import ru.posttrack.shared.TrackingNumber
import ru.posttrack.shared.UserId
import ru.posttrack.shared.WeightGrams
import ru.posttrack.shipment.Recipient
import ru.posttrack.shipment.Shipment
import ru.posttrack.shipment.ShipmentRepository
import ru.posttrack.shipment.ShipmentStatus
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class PostgresShipmentRepository(
    private val database: PostgresDatabase,
) : ShipmentRepository {
    override fun findByTrackingNumber(trackingNumber: TrackingNumber): Shipment? =
        find("WHERE s.tracking_number = ?", trackingNumber.value).singleOrNull()

    override fun findByRecipientPhone(phone: PhoneNumber): List<Shipment> =
        find("WHERE s.recipient_phone = ? ORDER BY s.created_at DESC, s.id", phone.value)

    override fun findByIdForUpdate(id: ShipmentId): Shipment? = find("WHERE s.id = ? FOR UPDATE OF s", id.value).singleOrNull()

    private fun find(
        condition: String,
        value: Any,
    ): List<Shipment> {
        val sql =
            """
            SELECT s.*, q.user_id AS quote_user_id,
                   q.from_index AS quote_from_index, q.to_index AS quote_to_index,
                   q.weight_grams AS quote_weight_grams, q.length_cm AS quote_length_cm,
                   q.width_cm AS quote_width_cm, q.height_cm AS quote_height_cm,
                   q.service_level AS quote_service_level, q.price_minor AS quote_price_minor,
                   q.currency AS quote_currency, q.delivery_from AS quote_delivery_from,
                   q.delivery_to AS quote_delivery_to, q.provider AS quote_provider,
                   q.product_code AS quote_product_code, q.created_at AS quote_created_at,
                   q.expires_at AS quote_expires_at
            FROM posttrack.shipments s
            JOIN posttrack.delivery_quotes q ON q.id = s.quote_id
            $condition
            """.trimIndent()
        return database.connection().prepareStatement(sql).use { statement ->
            statement.setObject(1, value)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(rows.toShipment())
                }
            }
        }
    }

    override fun save(shipment: Shipment) {
        database.insert(
            table = "posttrack.shipments",
            values =
                mapOf(
                    "id" to shipment.id.value,
                    "tracking_number" to shipment.trackingNumber.value,
                    "sender_id" to shipment.senderId.value,
                    "quote_id" to shipment.quote.id.value,
                    "recipient_name" to shipment.recipient.fullName,
                    "recipient_phone" to shipment.recipient.phone.value,
                    "recipient_email" to shipment.recipient.email?.value,
                    "from_index" to shipment.from.postalCode.value,
                    "from_region" to shipment.from.region,
                    "from_city" to shipment.from.city,
                    "from_address_line" to shipment.from.addressLine,
                    "to_index" to shipment.to.postalCode.value,
                    "to_region" to shipment.to.region,
                    "to_city" to shipment.to.city,
                    "to_address_line" to shipment.to.addressLine,
                    "declared_weight_grams" to shipment.parcel.weight.value,
                    "actual_weight_grams" to shipment.actualWeight?.value,
                    "status" to shipment.status.name,
                    "expected_delivery_date" to shipment.expectedDeliveryDate,
                    "created_at" to shipment.createdAt,
                    "updated_at" to shipment.updatedAt,
                ),
            updateColumns = listOf("actual_weight_grams", "status", "expected_delivery_date", "updated_at"),
        )
    }
}

private fun ResultSet.toShipment(): Shipment =
    Shipment.restore(
        id = ShipmentId(getObject("id", UUID::class.java)),
        trackingNumber = TrackingNumber.of(getString("tracking_number")),
        senderId = UserId(getObject("sender_id", UUID::class.java)),
        recipient =
            Recipient(
                fullName = getString("recipient_name"),
                phone = PhoneNumber.of(getString("recipient_phone")),
                email = getString("recipient_email")?.let(EmailAddress::of),
            ),
        from = toAddress("from"),
        to = toAddress("to"),
        quote = toDeliveryQuote("quote_"),
        status = ShipmentStatus.valueOf(getString("status")),
        actualWeight = (getObject("actual_weight_grams") as Int?)?.let(::WeightGrams),
        expectedDeliveryDate = getObject("expected_delivery_date", LocalDate::class.java),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

private fun ResultSet.toAddress(prefix: String): PostalAddress =
    PostalAddress(
        postalCode = PostalCode(getString("${prefix}_index")),
        region = getString("${prefix}_region"),
        city = getString("${prefix}_city"),
        addressLine = getString("${prefix}_address_line"),
    )
