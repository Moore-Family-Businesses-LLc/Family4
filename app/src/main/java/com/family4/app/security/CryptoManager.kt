package com.family4.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * End-to-end encryption utility for Family4 chat.
 *
 * Strategy:
 *  • Each device generates an AES-256-GCM key stored in Android Keystore
 *  • Messages are encrypted with AES-256-GCM before being stored/transmitted
 *  • IV is generated fresh for every message and stored alongside the ciphertext
 *  • The keystore key never leaves the secure hardware element
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val KEYSTORE_ALIAS = "family4_chat_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }

    /** Returns (or creates) the AES key from the secure keystore. */
    private fun getOrCreateKey(): SecretKey {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)   // can be set true for biometric gating
                    .build()
            )
            keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts [plaintext] and returns Pair(ciphertextBase64, ivBase64).
     */
    fun encrypt(plaintext: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP) to
               Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    /**
     * Decrypts [ciphertextBase64] using [ivBase64] and returns the plaintext.
     */
    fun decrypt(ciphertextBase64: String, ivBase64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}
