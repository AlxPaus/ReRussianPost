package ru.posttrack.app.infrastructure

import ru.posttrack.warehouse.PickupCodeHasher
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacPickupCodeHasher(
    secret: String,
) : PickupCodeHasher {
    private val key = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    init {
        require(secret.length >= 32) { "PICKUP_CODE_SECRET must contain at least 32 characters" }
    }

    override fun hash(code: String): String {
        val salt = UUID.randomUUID().toString()
        return "$salt:${digest(salt, code)}"
    }

    override fun matches(
        code: String,
        hash: String,
    ): Boolean {
        val parts = hash.split(':', limit = 2)
        return parts.size == 2 &&
            MessageDigest.isEqual(
                parts[1].toByteArray(Charsets.UTF_8),
                digest(parts[0], code).toByteArray(Charsets.UTF_8),
            )
    }

    private fun digest(
        salt: String,
        code: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return Base64.getEncoder().encodeToString(mac.doFinal("$salt:$code".toByteArray(Charsets.UTF_8)))
    }
}
