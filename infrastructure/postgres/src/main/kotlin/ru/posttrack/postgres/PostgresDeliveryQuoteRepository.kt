package ru.posttrack.postgres

import ru.posttrack.pricing.DeliveryQuote
import ru.posttrack.pricing.DeliveryQuoteRepository
import ru.posttrack.pricing.QuoteRequest
import ru.posttrack.pricing.ServiceLevel
import ru.posttrack.shared.DimensionsCm
import ru.posttrack.shared.Money
import ru.posttrack.shared.ParcelParameters
import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.QuoteId
import ru.posttrack.shared.UserId
import ru.posttrack.shared.WeightGrams
import java.sql.ResultSet
import java.time.LocalDate
import java.util.Currency
import java.util.UUID

class PostgresDeliveryQuoteRepository(
    private val database: PostgresDatabase,
) : DeliveryQuoteRepository {
    override fun findById(id: QuoteId): DeliveryQuote? {
        val sql = "SELECT * FROM posttrack.delivery_quotes WHERE id = ?"
        return database.connection().prepareStatement(sql).use { statement ->
            statement.setObject(1, id.value)
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.toDeliveryQuote() else null
            }
        }
    }

    override fun save(quote: DeliveryQuote) {
        database.insert(
            table = "posttrack.delivery_quotes",
            values =
                mapOf(
                    "id" to quote.id.value,
                    "user_id" to quote.userId.value,
                    "from_index" to quote.request.from.value,
                    "to_index" to quote.request.to.value,
                    "weight_grams" to quote.request.parcel.weight.value,
                    "length_cm" to quote.request.parcel.dimensions.length,
                    "width_cm" to quote.request.parcel.dimensions.width,
                    "height_cm" to quote.request.parcel.dimensions.height,
                    "service_level" to quote.request.serviceLevel.name,
                    "price_minor" to quote.price.amountMinor,
                    "currency" to quote.price.currency.currencyCode,
                    "delivery_from" to quote.deliveryFrom,
                    "delivery_to" to quote.deliveryTo,
                    "provider" to quote.provider,
                    "product_code" to quote.productCode,
                    "created_at" to quote.createdAt,
                    "expires_at" to quote.expiresAt,
                ),
        )
    }
}

internal fun ResultSet.toDeliveryQuote(prefix: String = ""): DeliveryQuote =
    DeliveryQuote(
        id = QuoteId(getObject("${prefix}id", UUID::class.java)),
        userId = UserId(getObject("${prefix}user_id", UUID::class.java)),
        request =
            QuoteRequest(
                from = PostalCode(getString("${prefix}from_index")),
                to = PostalCode(getString("${prefix}to_index")),
                parcel =
                    ParcelParameters(
                        weight = WeightGrams(getInt("${prefix}weight_grams")),
                        dimensions =
                            DimensionsCm(
                                length = getInt("${prefix}length_cm"),
                                width = getInt("${prefix}width_cm"),
                                height = getInt("${prefix}height_cm"),
                            ),
                    ),
                serviceLevel = ServiceLevel.valueOf(getString("${prefix}service_level")),
            ),
        price = Money(getLong("${prefix}price_minor"), Currency.getInstance(getString("${prefix}currency"))),
        deliveryFrom = getObject("${prefix}delivery_from", LocalDate::class.java),
        deliveryTo = getObject("${prefix}delivery_to", LocalDate::class.java),
        provider = getString("${prefix}provider"),
        productCode = getString("${prefix}product_code"),
        createdAt = getTimestamp("${prefix}created_at").toInstant(),
        expiresAt = getTimestamp("${prefix}expires_at").toInstant(),
    )
