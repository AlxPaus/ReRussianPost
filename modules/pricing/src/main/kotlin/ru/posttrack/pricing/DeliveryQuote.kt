package ru.posttrack.pricing

import ru.posttrack.shared.Money
import ru.posttrack.shared.ParcelParameters
import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.QuoteId
import ru.posttrack.shared.UserId
import java.time.Instant
import java.time.LocalDate

enum class ServiceLevel {
    STANDARD,
    EXPRESS,
}

data class QuoteRequest(
    val from: PostalCode,
    val to: PostalCode,
    val parcel: ParcelParameters,
    val serviceLevel: ServiceLevel,
)

data class QuoteCalculation(
    val price: Money,
    val deliveryFrom: LocalDate,
    val deliveryTo: LocalDate,
    val provider: String,
    val productCode: String,
) {
    init {
        require(!deliveryTo.isBefore(deliveryFrom)) { "Invalid delivery period" }
        require(provider.isNotBlank() && provider.length <= 64) { "Invalid quote provider" }
        require(productCode.isNotBlank() && productCode.length <= 64) { "Invalid product code" }
    }
}

data class DeliveryQuote(
    val id: QuoteId,
    val userId: UserId,
    val request: QuoteRequest,
    val price: Money,
    val deliveryFrom: LocalDate,
    val deliveryTo: LocalDate,
    val provider: String,
    val productCode: String,
    val createdAt: Instant,
    val expiresAt: Instant,
) {
    init {
        require(!deliveryTo.isBefore(deliveryFrom)) { "Invalid delivery period" }
        require(expiresAt.isAfter(createdAt)) { "Quote expiration must be after creation" }
        require(provider.isNotBlank() && provider.length <= 64) { "Invalid quote provider" }
        require(productCode.isNotBlank() && productCode.length <= 64) { "Invalid product code" }
    }

    fun isExpired(at: Instant): Boolean = !at.isBefore(expiresAt)

    companion object {
        fun create(
            id: QuoteId,
            userId: UserId,
            request: QuoteRequest,
            calculation: QuoteCalculation,
            createdAt: Instant,
            expiresAt: Instant,
        ): DeliveryQuote =
            DeliveryQuote(
                id = id,
                userId = userId,
                request = request,
                price = calculation.price,
                deliveryFrom = calculation.deliveryFrom,
                deliveryTo = calculation.deliveryTo,
                provider = calculation.provider,
                productCode = calculation.productCode,
                createdAt = createdAt,
                expiresAt = expiresAt,
            )
    }
}

fun interface DeliveryQuoteProvider {
    suspend fun calculate(request: QuoteRequest): QuoteCalculation
}
