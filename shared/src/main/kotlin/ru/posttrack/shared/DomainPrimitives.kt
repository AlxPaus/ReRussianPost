package ru.posttrack.shared

import java.util.Currency
import java.util.UUID

class InvalidValue(
    field: String,
    reason: String,
) : IllegalArgumentException("Invalid $field: $reason")

@JvmInline
value class UserId(
    val value: UUID,
) {
    companion object {
        fun random(): UserId = UserId(UUID.randomUUID())
    }
}

@JvmInline
value class QuoteId(
    val value: UUID,
) {
    companion object {
        fun random(): QuoteId = QuoteId(UUID.randomUUID())
    }
}

@JvmInline
value class ShipmentId(
    val value: UUID,
) {
    companion object {
        fun random(): ShipmentId = ShipmentId(UUID.randomUUID())
    }
}

@JvmInline
value class TrackingEventId(
    val value: UUID,
) {
    companion object {
        fun random(): TrackingEventId = TrackingEventId(UUID.randomUUID())
    }
}

@JvmInline
value class StorageCellId(
    val value: UUID,
) {
    companion object {
        fun random(): StorageCellId = StorageCellId(UUID.randomUUID())
    }
}

@JvmInline
value class TrackingNumber private constructor(
    val value: String,
) {
    companion object {
        private val TRACKING_NUMBER_PATTERN = Regex("[A-Z0-9]{10,32}")

        fun of(raw: String): TrackingNumber {
            val normalized = raw.trim().uppercase()
            if (!normalized.matches(TRACKING_NUMBER_PATTERN)) {
                throw InvalidValue("trackingNumber", "must contain 10 to 32 uppercase letters or digits")
            }
            return TrackingNumber(normalized)
        }
    }
}

data class Money(
    val amountMinor: Long,
    val currency: Currency = RUB,
) {
    init {
        if (amountMinor < 0) throw InvalidValue("amountMinor", "must not be negative")
    }

    companion object {
        val RUB: Currency = Currency.getInstance("RUB")
    }
}

@JvmInline
value class PostalCode(
    val value: String,
) {
    init {
        if (!value.matches(POSTAL_CODE_PATTERN)) {
            throw InvalidValue("postalCode", "must contain exactly 6 digits")
        }
    }

    companion object {
        private val POSTAL_CODE_PATTERN = Regex("\\d{6}")
    }
}

@JvmInline
value class WeightGrams(
    val value: Int,
) {
    init {
        if (value <= 0) throw InvalidValue("weightGrams", "must be greater than zero")
        if (value > MAX_PARCEL_WEIGHT_GRAMS) {
            throw InvalidValue("weightGrams", "must not exceed $MAX_PARCEL_WEIGHT_GRAMS")
        }
    }

    companion object {
        const val MAX_PARCEL_WEIGHT_GRAMS = 50_000
    }
}

data class DimensionsCm(
    val length: Int,
    val width: Int,
    val height: Int,
) {
    init {
        listOf("length" to length, "width" to width, "height" to height).forEach { (name, value) ->
            if (value <= 0) throw InvalidValue(name, "must be greater than zero")
            if (value > MAX_DIMENSION_CM) throw InvalidValue(name, "must not exceed $MAX_DIMENSION_CM cm")
        }

        if (length + width + height > MAX_DIMENSIONS_SUM_CM) {
            throw InvalidValue("dimensions", "sum must not exceed $MAX_DIMENSIONS_SUM_CM cm")
        }
    }

    companion object {
        const val MAX_DIMENSION_CM = 150
        const val MAX_DIMENSIONS_SUM_CM = 300
    }
}

data class ParcelParameters(
    val weight: WeightGrams,
    val dimensions: DimensionsCm,
)

@JvmInline
value class EmailAddress private constructor(
    val value: String,
) {
    companion object {
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun of(raw: String): EmailAddress {
            val normalized = raw.trim().lowercase()
            if (normalized.length > 320 || !normalized.matches(EMAIL_PATTERN)) {
                throw InvalidValue("email", "must be a valid email address")
            }
            return EmailAddress(normalized)
        }
    }
}

@JvmInline
value class PhoneNumber private constructor(
    val value: String,
) {
    companion object {
        private val E164_PATTERN = Regex("^\\+[1-9]\\d{7,14}$")

        fun of(raw: String): PhoneNumber {
            val normalized = raw.filterNot(Char::isWhitespace)
            if (!normalized.matches(E164_PATTERN)) {
                throw InvalidValue("phone", "must use E.164 format, for example +79991234567")
            }
            return PhoneNumber(normalized)
        }
    }
}

data class PostalAddress(
    val postalCode: PostalCode,
    val region: String,
    val city: String,
    val addressLine: String,
) {
    init {
        requireText("region", region)
        requireText("city", city)
        requireText("addressLine", addressLine)
    }
}

private fun requireText(
    field: String,
    value: String,
) {
    if (value.isBlank()) throw InvalidValue(field, "must not be blank")
    if (value.length > 255) throw InvalidValue(field, "must not exceed 255 characters")
}
