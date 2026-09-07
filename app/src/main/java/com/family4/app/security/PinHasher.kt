package com.family4.app.security

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * One-way hashing for the app-unlock PIN.
 *
 * A 4–6 digit PIN has a tiny keyspace, so the defence is deliberate slowness:
 * PBKDF2-HMAC-SHA256 with a per-PIN random salt and a high iteration count.
 * Stored form is `pbkdf2$<iterations>$<saltB64>$<hashB64>` — self-describing,
 * so the iteration count can be raised later without breaking existing PINs.
 *
 * The plaintext PIN is never persisted. Verification is constant-time.
 */
object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val PREFIX = "pbkdf2"
    private const val SEPARATOR = "$"

    /** Hashes [pin] with a fresh random salt. Safe to store. */
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = derive(pin, salt, ITERATIONS)
        return listOf(
            PREFIX,
            ITERATIONS.toString(),
            encode(salt),
            encode(digest)
        ).joinToString(SEPARATOR)
    }

    /**
     * Checks [pin] against a value produced by [hash].
     *
     * Returns false for malformed or legacy plaintext values rather than
     * throwing — a corrupt record must not lock the user out with a crash.
     */
    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 4 || parts[0] != PREFIX) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = decodeOrNull(parts[2]) ?: return false
        val expected = decodeOrNull(parts[3]) ?: return false

        return constantTimeEquals(derive(pin, salt, iterations), expected)
    }

    /** True when [stored] was written by an older build as plaintext. */
    fun isLegacyPlaintext(stored: String): Boolean =
        stored.isNotEmpty() && !stored.startsWith("$PREFIX$SEPARATOR")

    // ── Internals ────────────────────────────────────────────────────────────

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decodeOrNull(value: String): ByteArray? =
        try { Base64.decode(value, Base64.NO_WRAP) } catch (_: IllegalArgumentException) { null }

    /** Comparison that does not short-circuit on the first differing byte. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
