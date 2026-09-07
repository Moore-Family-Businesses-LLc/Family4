@file:Suppress("DEPRECATION") // GoogleSignIn is deprecated in favour of Credential Manager; migration is a larger refactor
package com.family4.app.ui.settings

import android.app.Application
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.ChatDao
import com.family4.app.data.prefs.SettingsKeys
import com.family4.app.data.prefs.settingsDataStore
import com.family4.app.security.PinHasher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val chatDao: ChatDao
) : AndroidViewModel(app) {

    /** Shared process-wide store — see com.family4.app.data.prefs. */
    private val ds = app.settingsDataStore

    companion object {
        // Aliases kept so existing call sites (and the handoff doc) still
        // resolve; the keys themselves now live in SettingsKeys.
        val KEY_NOTIFICATIONS       = SettingsKeys.NOTIFICATIONS
        val KEY_LOCATION_SHARING    = SettingsKeys.LOCATION_SHARING
        val KEY_BIOMETRIC           = SettingsKeys.BIOMETRIC
        val KEY_DRIVE_BACKUP        = SettingsKeys.DRIVE_BACKUP
        val KEY_SOS_AUTO_CALL       = SettingsKeys.SOS_AUTO_CALL
        val KEY_TEMP_UNIT           = SettingsKeys.TEMP_UNIT
        val KEY_DARK_MODE           = SettingsKeys.DARK_MODE
        val KEY_FONT_SIZE           = SettingsKeys.FONT_SIZE
        val KEY_CHAT_RETENTION_DAYS = SettingsKeys.CHAT_RETENTION_DAYS
        val KEY_APP_PIN_ENABLED     = SettingsKeys.APP_PIN_ENABLED
        val KEY_APP_PIN             = SettingsKeys.APP_PIN
        val KEY_WALKIE_CHANNEL      = SettingsKeys.WALKIE_CHANNEL
        val KEY_LOCATION_INTERVAL   = SettingsKeys.LOCATION_INTERVAL
    }

    // ── Observable settings ──────────────────────────────────────────────────

    val notificationsEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_NOTIFICATIONS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val locationSharingEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_LOCATION_SHARING] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val biometricEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_BIOMETRIC] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val driveBackupEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_DRIVE_BACKUP] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val sosAutoCallEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_SOS_AUTO_CALL] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val temperatureUnit: StateFlow<String> = ds.data
        .map { it[KEY_TEMP_UNIT] ?: "F" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "F")

    val darkMode: StateFlow<String> = ds.data
        .map { it[KEY_DARK_MODE] ?: "dark" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    val fontSize: StateFlow<String> = ds.data
        .map { it[KEY_FONT_SIZE] ?: "medium" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "medium")

    val chatRetentionDays: StateFlow<Int> = ds.data
        .map { it[KEY_CHAT_RETENTION_DAYS] ?: 30 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    val appPinEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_APP_PIN_ENABLED] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val walkieChannel: StateFlow<Int> = ds.data
        .map { it[KEY_WALKIE_CHANNEL] ?: 1 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val locationInterval: StateFlow<Int> = ds.data
        .map { it[KEY_LOCATION_INTERVAL] ?: 60 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 60)

    // ── Setters ──────────────────────────────────────────────────────────────

    fun setNotifications(enabled: Boolean)    = setPref(KEY_NOTIFICATIONS, enabled)
    fun setLocationSharing(enabled: Boolean)  = setPref(KEY_LOCATION_SHARING, enabled)
    fun setBiometric(enabled: Boolean)        = setPref(KEY_BIOMETRIC, enabled)
    fun setDriveBackup(enabled: Boolean)      = setPref(KEY_DRIVE_BACKUP, enabled)
    fun setSosAutoCall(enabled: Boolean)      = setPref(KEY_SOS_AUTO_CALL, enabled)
    fun setAppPinEnabled(enabled: Boolean)    = setPref(KEY_APP_PIN_ENABLED, enabled)

    fun setTemperatureUnit(unit: String) = viewModelScope.launch {
        ds.edit { it[KEY_TEMP_UNIT] = if (unit == "C") "C" else "F" }
    }

    fun setDarkMode(mode: String) = viewModelScope.launch {
        ds.edit { it[KEY_DARK_MODE] = mode }
    }

    fun setFontSize(size: String) = viewModelScope.launch {
        ds.edit { it[KEY_FONT_SIZE] = size }
    }

    fun setChatRetentionDays(days: Int) = viewModelScope.launch {
        ds.edit { it[KEY_CHAT_RETENTION_DAYS] = days }
        applyRetentionPolicy(days)
    }

    fun setWalkieChannel(ch: Int) = viewModelScope.launch {
        ds.edit { it[KEY_WALKIE_CHANNEL] = ch.coerceIn(1, 99) }
    }

    fun setLocationInterval(sec: Int) = viewModelScope.launch {
        ds.edit { it[KEY_LOCATION_INTERVAL] = sec }
    }

    // ── App PIN (hashed, never stored in plaintext) ──────────────────────────

    /** Stores a PBKDF2 digest of [pin]; the PIN itself is discarded. */
    fun setAppPin(pin: String) = viewModelScope.launch {
        val digest = PinHasher.hash(pin)
        ds.edit { it[KEY_APP_PIN] = digest }
    }

    /**
     * Verifies an entered PIN against the stored digest.
     *
     * Returns false when no PIN has been set. A digest written by an older
     * build (plaintext) is compared directly once and then upgraded in place,
     * so existing users are not locked out by this change.
     */
    suspend fun verifyAppPin(pin: String): Boolean {
        val stored = ds.data.map { it[KEY_APP_PIN] }.first() ?: return false
        if (stored.isEmpty()) return false

        if (PinHasher.isLegacyPlaintext(stored)) {
            if (stored != pin) return false
            ds.edit { it[KEY_APP_PIN] = PinHasher.hash(pin) }   // migrate on success
            return true
        }
        return PinHasher.verify(pin, stored)
    }

    /** True once a PIN digest exists — used to gate the unlock screen. */
    suspend fun hasAppPin(): Boolean =
        !ds.data.map { it[KEY_APP_PIN] }.first().isNullOrEmpty()

    // ── Maintenance ──────────────────────────────────────────────────────────

    /** Deletes every stored message. Irreversible — confirm before calling. */
    fun clearChatHistory() = viewModelScope.launch {
        chatDao.deleteAllMessages()
    }

    /** Drops messages older than the retention window (0 = keep forever). */
    private suspend fun applyRetentionPolicy(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        chatDao.deleteMessagesOlderThan(cutoff)
    }

    fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(app, gso).signOut()
    }

    private fun setPref(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch { ds.edit { it[key] = value } }
    }
}
