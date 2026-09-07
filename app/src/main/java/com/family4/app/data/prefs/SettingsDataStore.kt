package com.family4.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * The app's single Preferences DataStore.
 *
 * IMPORTANT: exactly one `preferencesDataStore` delegate may exist per file
 * name per process — declaring a second one for "family4_settings" elsewhere
 * throws at runtime ("There are multiple DataStores active..."). Every screen
 * that needs settings must use this delegate.
 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SettingsKeys.STORE_NAME
)

/**
 * Preference keys shared across features (Settings writes them; Weather,
 * Walkie-Talkie and the location service read them).
 */
object SettingsKeys {

    const val STORE_NAME = "family4_settings"

    val NOTIFICATIONS       = booleanPreferencesKey("notifications_enabled")
    val LOCATION_SHARING    = booleanPreferencesKey("location_sharing_enabled")
    val BIOMETRIC           = booleanPreferencesKey("biometric_enabled")
    val DRIVE_BACKUP        = booleanPreferencesKey("drive_backup_enabled")
    val SOS_AUTO_CALL       = booleanPreferencesKey("sos_auto_call_enabled")

    /** "C" | "F" — default "F". */
    val TEMP_UNIT           = stringPreferencesKey("temperature_unit")
    /** "dark" | "light" | "system" — default "dark". */
    val DARK_MODE           = stringPreferencesKey("dark_mode")
    /** "small" | "medium" | "large" — default "medium". */
    val FONT_SIZE           = stringPreferencesKey("font_size")
    /** 7 | 30 | 90 | 0 = forever — default 30. */
    val CHAT_RETENTION_DAYS = intPreferencesKey("chat_retention_days")

    val APP_PIN_ENABLED     = booleanPreferencesKey("app_pin_enabled")
    /** PBKDF2 digest of the PIN — never the PIN itself. See PinHasher. */
    val APP_PIN             = stringPreferencesKey("app_pin")

    /** 1–99 — default 1. */
    val WALKIE_CHANNEL      = intPreferencesKey("walkie_channel")
    /** Seconds: 30 | 60 | 300 — default 60. */
    val LOCATION_INTERVAL   = intPreferencesKey("location_interval_sec")
}
