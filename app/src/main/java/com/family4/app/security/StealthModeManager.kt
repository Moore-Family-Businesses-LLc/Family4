package com.family4.app.security

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.family4.app.R
import com.family4.app.data.prefs.settingsDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * StealthModeManager — controls whether the app is visible or hidden behind a
 * calculator UI. Parents unlock with a secret tap pattern; the pattern rotates
 * every 14 days and a push notification reminds them to re-set it.
 *
 * Pattern storage: SHA-256 of the dot-sequence string (e.g. "1,5,9,3") —
 * never stored in plaintext.
 */
object StealthModeManager {

    private const val CHANNEL_ID      = "stealth_rotation"
    private const val NOTIF_ID        = 7001
    private const val ROTATION_MS     = 14L * 24 * 60 * 60 * 1000  // 14 days

    private val KEY_STEALTH_ENABLED   = booleanPreferencesKey("stealth_enabled")
    private val KEY_PATTERN_HASH      = stringPreferencesKey("stealth_pattern_hash")
    private val KEY_PATTERN_SET_AT    = longPreferencesKey("stealth_pattern_set_at")
    private val KEY_IS_UNLOCKED       = booleanPreferencesKey("stealth_unlocked")

    // ── Public API ────────────────────────────────────────────────────────

    fun isStealthEnabled(ctx: Context): Boolean = runBlocking {
        ctx.settingsDataStore.data.first()[KEY_STEALTH_ENABLED] ?: false
    }

    fun isUnlocked(ctx: Context): Boolean = runBlocking {
        ctx.settingsDataStore.data.first()[KEY_IS_UNLOCKED] ?: true
    }

    suspend fun setStealthEnabled(ctx: Context, enabled: Boolean) {
        ctx.settingsDataStore.edit { it[KEY_STEALTH_ENABLED] = enabled }
        if (enabled) scheduleRotationAlarm(ctx)
        else cancelRotationAlarm(ctx)
    }

    suspend fun setPattern(ctx: Context, dots: List<Int>) {
        ctx.settingsDataStore.edit {
            it[KEY_PATTERN_HASH] = hash(dots)
            it[KEY_PATTERN_SET_AT] = System.currentTimeMillis()
        }
    }

    fun verifyPattern(ctx: Context, dots: List<Int>): Boolean {
        val stored = runBlocking {
            ctx.settingsDataStore.data.first()[KEY_PATTERN_HASH]
        } ?: return false
        return hash(dots) == stored
    }

    suspend fun unlock(ctx: Context) {
        ctx.settingsDataStore.edit { it[KEY_IS_UNLOCKED] = true }
    }

    suspend fun lock(ctx: Context) {
        ctx.settingsDataStore.edit { it[KEY_IS_UNLOCKED] = false }
    }

    fun isPatternExpired(ctx: Context): Boolean = runBlocking {
        val setAt = ctx.settingsDataStore.data.first()[KEY_PATTERN_SET_AT] ?: return@runBlocking false
        System.currentTimeMillis() - setAt > ROTATION_MS
    }

    // ── 14-day rotation alarm ─────────────────────────────────────────────

    fun scheduleRotationAlarm(ctx: Context) {
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            ctx, 8001,
            Intent(ctx, StealthRotationReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + ROTATION_MS,
            ROTATION_MS,
            intent
        )
    }

    fun cancelRotationAlarm(ctx: Context) {
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            ctx, 8001,
            Intent(ctx, StealthRotationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmMgr.cancel(intent)
    }

    fun sendRotationNotification(ctx: Context) {
        ensureChannel(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("Family4 — Update Stealth Pattern")
            .setContentText("Your 14-day security window has passed. Tap to set a new unlock pattern.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun hash(dots: List<Int>): String {
        val input = dots.joinToString(",").toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(input)
            .joinToString("") { "%02x".format(it) }
    }

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Stealth Mode Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Reminds parents to rotate the stealth unlock pattern" }
        nm.createNotificationChannel(ch)
    }
}
