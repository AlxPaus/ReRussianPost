package ru.posttrack.app.http

import kotlinx.serialization.Serializable
import ru.posttrack.pricing.DeliveryQuote
import ru.posttrack.pricing.QuoteRequest
import ru.posttrack.pricing.ServiceLevel
import ru.posttrack.shared.PostalCode

@Serializable
data class CalculateQuoteRequest(
    val from: String,
    val to: String,
    val parcel: ParcelDto,
    val serviceLevel: String,
) {
    fun toDomain(): QuoteRequest = QuoteRequest(PostalCode(from), PostalCode(to), parcel.toDomain(), ServiceLevel.valueOf(serviceLevel))
}

@Serializable
data class QuoteResponse(
    val id: String,
    val from: String,
    val to: String,
    val parcel: ParcelDto,
    val serviceLevel: String,
    val priceMinor: Long,
    val currency: String,
    val deliveryFrom: String,
    val deliveryTo: String,
    val expiresAt: String,
)

fun DeliveryQuote.toResponse(): QuoteResponse =
    QuoteResponse(
        id = id.value.toString(),
        from = request.from.value,
        to = request.to.value,
        parcel = request.parcel.toDto(),
        serviceLevel = request.serviceLevel.name,
        priceMinor = price.amountMinor,
        currency = price.currency.currencyCode,
        deliveryFrom = deliveryFrom.toString(),
        deliveryTo = deliveryTo.toString(),
        expiresAt = expiresAt.toString(),
    )
