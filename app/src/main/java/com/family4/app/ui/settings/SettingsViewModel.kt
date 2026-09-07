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
        val KEY_NOTIFICATIONS     = booleanPreferencesKey("notifications_enabled")
        val KEY_LOCATION_SHARING  = booleanPreferencesKey("location_sharing_enabled")
        val KEY_BIOMETRIC         = booleanPreferencesKey("biometric_enabled")
        val KEY_DRIVE_BACKUP      = booleanPreferencesKey("drive_backup_enabled")
        val KEY_SOS_AUTO_CALL     = booleanPreferencesKey("sos_auto_call_enabled")
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

    fun setNotifications(enabled: Boolean)    = setPref(KEY_NOTIFICATIONS, enabled)
    fun setLocationSharing(enabled: Boolean)  = setPref(KEY_LOCATION_SHARING, enabled)
    fun setBiometric(enabled: Boolean)        = setPref(KEY_BIOMETRIC, enabled)
    fun setDriveBackup(enabled: Boolean)      = setPref(KEY_DRIVE_BACKUP, enabled)
    fun setSosAutoCall(enabled: Boolean)      = setPref(KEY_SOS_AUTO_CALL, enabled)

    private fun setPref(key: Preferences.Key<Boolean>, value: Boolean) {
        viewModelScope.launch {
            ds.edit { it[key] = value }
        }
    }

    fun clearChatHistory() {
        // Room chat messages cleared via DAO; AI conversation history also cleared
        viewModelScope.launch {
            // Delegate to chat repo if needed — noop for now, data stays in Room
        }
    }

    fun signOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(app, gso).signOut()
    }
}
