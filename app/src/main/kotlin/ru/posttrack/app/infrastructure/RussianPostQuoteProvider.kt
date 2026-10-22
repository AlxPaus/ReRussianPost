package ru.posttrack.app.infrastructure

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.posttrack.pricing.DeliveryQuoteProvider
import ru.posttrack.pricing.QuoteCalculation
import ru.posttrack.pricing.QuoteRequest
import ru.posttrack.pricing.ServiceLevel
import ru.posttrack.shared.Money
import java.time.Clock
import java.time.LocalDate

class QuoteProviderUnavailable : RuntimeException("Russian Post API credentials are not configured")

class QuoteProviderFailure(
    cause: Throwable? = null,
) : RuntimeException("Russian Post could not return a delivery quote", cause)

class RussianPostQuoteProvider(
    private val client: HttpClient,
    private val token: String?,
    private val userKey: String?,
    private val clock: Clock,
) : DeliveryQuoteProvider {
    override suspend fun calculate(request: QuoteRequest): QuoteCalculation {
        if (token.isNullOrBlank() || userKey.isNullOrBlank()) throw QuoteProviderUnavailable()
        val product = if (request.serviceLevel == ServiceLevel.STANDARD) "POSTAL_PARCEL" else "EMS"
        val dimensions = request.parcel.dimensions
        val response =
            try {
                client.post("https://otpravka-api.pochta.ru/1.0/tariff") {
                    header("Authorization", "AccessToken $token")
                    header("X-User-Authorization", "Basic $userKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        TariffRequest(
                            request.from.value,
                            request.to.value,
                            product,
                            request.parcel.weight.value,
                            TariffDimensions(dimensions.length, dimensions.width, dimensions.height),
                        ),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                throw QuoteProviderFailure(error)
            }
        if (response.status.value == 400 || response.status.value == 422) {
            throw IllegalArgumentException("Russian Post rejected the route or parcel parameters")
        }
        if (!response.status.isSuccess()) throw QuoteProviderFailure()
        return try {
            val tariff = response.body<TariffResponse>()
            val delivery = requireNotNull(tariff.deliveryTime) { "Missing delivery period" }
            val minDays = delivery.minDays ?: delivery.maxDays
            require(minDays >= 0 && delivery.maxDays >= minDays)
            require(tariff.totalRate >= 0 && tariff.totalVat >= 0)
            val today = LocalDate.now(clock)
            QuoteCalculation(
                price = Money(Math.addExact(tariff.totalRate, tariff.totalVat)),
                deliveryFrom = today.plusDays(minDays.toLong()),
                deliveryTo = today.plusDays(delivery.maxDays.toLong()),
                provider = "RUSSIAN_POST",
                productCode = product,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw QuoteProviderFailure(error)
        }
    }
}

fun russianPostClient(): HttpClient =
    HttpClient(CIO) {
        followRedirects = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
    }

@Serializable
private data class TariffRequest(
    @SerialName("index-from") val from: String,
    @SerialName("index-to") val to: String,
    @SerialName("mail-type") val mailType: String,
    val mass: Int,
    val dimension: TariffDimensions,
    @SerialName("mail-category") val mailCategory: String = "ORDINARY",
)

@Serializable
private data class TariffDimensions(
    val length: Int,
    val width: Int,
    val height: Int,
)

@Serializable
private data class TariffResponse(
    @SerialName("total-rate") val totalRate: Long,
    @SerialName("total-vat") val totalVat: Long,
    @SerialName("delivery-time") val deliveryTime: DeliveryTime? = null,
)

@Serializable
private data class DeliveryTime(
    @SerialName("max-days") val maxDays: Int,
    @SerialName("min-days") val minDays: Int? = null,
)
