package com.family4.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "family4_settings")

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val ds = app.dataStore

    companion object {
        val KEY_NOTIFICATIONS       = booleanPreferencesKey("notifications_enabled")
        val KEY_LOCATION_SHARING    = booleanPreferencesKey("location_sharing_enabled")
        val KEY_BIOMETRIC           = booleanPreferencesKey("biometric_enabled")
        val KEY_DRIVE_BACKUP        = booleanPreferencesKey("drive_backup_enabled")
        val KEY_SOS_AUTO_CALL       = booleanPreferencesKey("sos_auto_call_enabled")
        // New preferences
        val KEY_TEMP_UNIT           = stringPreferencesKey("temperature_unit")   // "C" | "F"
        val KEY_DARK_MODE           = stringPreferencesKey("dark_mode")          // "dark" | "light" | "system"
        val KEY_FONT_SIZE           = stringPreferencesKey("font_size")          // "small" | "medium" | "large"
        val KEY_CHAT_RETENTION_DAYS = intPreferencesKey("chat_retention_days")   // 7 | 30 | 90 | 0=forever
        val KEY_APP_PIN_ENABLED     = booleanPreferencesKey("app_pin_enabled")
        val KEY_APP_PIN             = stringPreferencesKey("app_pin")            // 4-6 digit PIN (hashed in prod)
        val KEY_WALKIE_CHANNEL      = intPreferencesKey("walkie_channel")        // 1-99
        val KEY_LOCATION_INTERVAL   = intPreferencesKey("location_interval_sec") // 30 | 60 | 300
    }

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

    // ── Setters ───────────────────────────────────────────────────────────────
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
    }
    fun setAppPin(pin: String) = viewModelScope.launch {
        ds.edit { it[KEY_APP_PIN] = pin }
    }
    fun setWalkieChannel(ch: Int) = viewModelScope.launch {
        ds.edit { it[KEY_WALKIE_CHANNEL] = ch.coerceIn(1, 99) }
    }
    fun setLocationInterval(sec: Int) = viewModelScope.launch {
        ds.edit { it[KEY_LOCATION_INTERVAL] = sec }
    }

    private fun setPref(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch { ds.edit { it[key] = value } }
    }

    fun clearChatHistory() = viewModelScope.launch { /* stub — wired to ChatDao in prod */ }

    fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(app, gso).signOut()
    }
}
