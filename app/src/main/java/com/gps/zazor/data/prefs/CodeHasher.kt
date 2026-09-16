package com.gps.zazor.data.prefs

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted, stretched hashes for the passcode and the wipe code.
 *
 * Both codes are only ever compared, never shown back, so there is no reason to keep them readable
 * and every reason not to: a plain string in preferences is one backup or one forensic dump away
 * from being read, and this app sells itself on privacy. A per-code salt and a slow derivation turn
 * reading into guessing.
 *
 * What this does not do is make a five-digit code strong - a hundred thousand candidates fall to an
 * offline search whatever the hash. It moves the floor from "anyone who opens the file can read the
 * code" to "someone has to run a search for it", which is the least an app like this should do.
 *
 * Hex rather than `android.util.Base64` on purpose: this is plain Java, so it can be unit-tested
 * without an emulator. PBKDF2-HMAC-SHA1 rather than SHA256 because the SHA256 variant only arrives
 * with API 26 and the app supports 24.
 */
object CodeHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val SEPARATOR = ":"

    /** @return `salt:hash`, both hex. */
    fun hash(code: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return salt.toHex() + SEPARATOR + derive(code, salt).toHex()
    }

    /** True when [stored] was made from [code]. */
    fun matches(code: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return false
        val salt = parts[0].fromHex() ?: return false
        val expected = parts[1].fromHex() ?: return false
        return MessageDigest.isEqual(derive(code, salt), expected)
    }

    /** False for a value written before codes were hashed, which is stored as the code itself. */
    fun isHashed(stored: String): Boolean = stored.split(SEPARATOR).size == 2

    private fun derive(code: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance(ALGORITHM)
            .generateSecret(PBEKeySpec(code.toCharArray(), salt, ITERATIONS, KEY_BITS))
            .encoded

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray? {
        if (length % 2 != 0) return null
        return try {
            ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
