package com.family4.app.vehicle.bluelink

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Hyundai BlueLink OAuth 2.0 tokens.
 *
 * Tokens are stored in [EncryptedSharedPreferences] (AES-256-GCM via Android Keystore).
 * The access token is short-lived (~30 min); this manager auto-refreshes it
 * when [getValidAccessToken] detects expiry.
 *
 * To use with real credentials:
 *  1. Apply at developer.hyundai.com and receive CLIENT_ID + CLIENT_SECRET.
 *  2. Add to local.properties:
 *       BLUELINK_CLIENT_ID=your_id
 *       BLUELINK_CLIENT_SECRET=your_secret
 *  3. Implement the OAuth web-view flow to obtain the initial authorization code.
 *
 * Until credentials are available this class reports [isAuthenticated] = false
 * and [BlueLinkRepository] serves stub data.
 */
@Singleton
class BlueLinkAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BlueLinkAuth"
        private const val PREFS_FILE  = "bluelink_tokens"
        private const val KEY_ACCESS  = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_EXPIRY  = "token_expiry_ms"
        private const val KEY_VIN_ID  = "selected_vin_id"
        private const val REFRESH_BUFFER_MS = 5 * 60 * 1000L // refresh 5 min before expiry
    }

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, PREFS_FILE, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** True when a refresh token is stored (user has linked their Hyundai account). */
    val isAuthenticated: Boolean
        get() = prefs.getString(KEY_REFRESH, null) != null

    /** The currently selected vehicle ID (VIN-derived ID from Hyundai API). */
    var selectedVinId: String
        get()      = prefs.getString(KEY_VIN_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_VIN_ID, value).apply()

    /**
     * Returns a valid Bearer token for API calls, auto-refreshing if needed.
     * Returns null if not authenticated or refresh fails.
     */
    suspend fun getValidAccessToken(api: BlueLinkApi, clientId: String, clientSecret: String): String? {
        if (!isAuthenticated) return null

        val expiryMs = prefs.getLong(KEY_EXPIRY, 0L)
        val now      = System.currentTimeMillis()

        if (now < expiryMs - REFRESH_BUFFER_MS) {
            // Still valid
            return prefs.getString(KEY_ACCESS, null)
        }

        // Refresh
        val refreshToken = prefs.getString(KEY_REFRESH, null) ?: return null
        return try {
            val resp = api.refreshToken(
                refreshToken = refreshToken,
                clientId     = clientId,
                clientSecret = clientSecret
            )
            if (resp.isSuccessful) {
                val body = resp.body()!!
                storeTokens(body.access_token, body.refresh_token, body.expires_in)
                body.access_token
            } else {
                Log.w(TAG, "Token refresh failed: ${resp.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error: ${e.message}")
            null
        }
    }

    /** Called by the OAuth web-view flow after receiving the authorization code. */
    suspend fun handleAuthorizationCode(
        api: BlueLinkApi, code: String, redirectUri: String,
        clientId: String, clientSecret: String
    ): Boolean {
        return try {
            val resp = api.exchangeToken(
                code         = code,
                redirectUri  = redirectUri,
                clientId     = clientId,
                clientSecret = clientSecret
            )
            if (resp.isSuccessful) {
                val body = resp.body()!!
                storeTokens(body.access_token, body.refresh_token, body.expires_in)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Auth code exchange error: ${e.message}")
            false
        }
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    private fun storeTokens(access: String, refresh: String, expiresInSec: Int) {
        val expiry = System.currentTimeMillis() + expiresInSec * 1000L
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putLong(KEY_EXPIRY, expiry)
            .apply()
    }
}
