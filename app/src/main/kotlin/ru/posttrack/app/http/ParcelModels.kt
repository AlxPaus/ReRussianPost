package ru.posttrack.app.http

import kotlinx.serialization.Serializable
import ru.posttrack.shared.DimensionsCm
import ru.posttrack.shared.ParcelParameters
import ru.posttrack.shared.PostalAddress
import ru.posttrack.shared.PostalCode
import ru.posttrack.shared.WeightGrams

@Serializable
data class ParcelDto(
    val weightGrams: Int,
    val lengthCm: Int,
    val widthCm: Int,
    val heightCm: Int,
) {
    fun toDomain(): ParcelParameters = ParcelParameters(WeightGrams(weightGrams), DimensionsCm(lengthCm, widthCm, heightCm))
}

@Serializable
data class AddressDto(
    val postalCode: String,
    val region: String,
    val city: String,
    val addressLine: String,
) {
    fun toDomain(): PostalAddress = PostalAddress(PostalCode(postalCode), region, city, addressLine)
}

fun ParcelParameters.toDto(): ParcelDto = ParcelDto(weight.value, dimensions.length, dimensions.width, dimensions.height)

fun PostalAddress.toDto(): AddressDto = AddressDto(postalCode.value, region, city, addressLine)
