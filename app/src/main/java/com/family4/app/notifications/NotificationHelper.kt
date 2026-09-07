package com.family4.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.family4.app.R
import com.family4.app.ui.main.MainActivity

/**
 * NotificationHelper — central hub for ALL push and local notifications in Family4.
 *
 * Channels:
 *  CHANNEL_CHAT     — encrypted messages (HIGH priority, heads-up)
 *  CHANNEL_SOS      — emergency alerts (MAX priority, always heads-up)
 *  CHANNEL_LOCATION — location sharing updates (LOW priority, silent)
 *  CHANNEL_WALKIE   — walkie-talkie status (LOW priority)
 *  CHANNEL_GENERAL  — task reminders, calendar events, system (DEFAULT)
 *  CHANNEL_DRIVE    — Drive sync status (MIN priority)
 */
object NotificationHelper {

    // ── Channel IDs ──────────────────────────────────────────────────────────
    const val CHANNEL_CHAT     = "family4_chat"
    const val CHANNEL_SOS      = "family4_sos"
    const val CHANNEL_LOCATION = "family4_location"
    const val CHANNEL_WALKIE   = "family4_walkie"
    const val CHANNEL_GENERAL  = "family4_general"
    const val CHANNEL_DRIVE    = "family4_drive"

    // ── Notification IDs ─────────────────────────────────────────────────────
    private var notifIdCounter = 100

    /**
     * MUST be called once in Application.onCreate() to register all channels.
     */
    fun createAllChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_CHAT, "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Encrypted family messages"
            enableVibration(true)
            enableLights(true)
            lightColor = 0xFF00D4FF.toInt()
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_SOS, "Emergency SOS",
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "Emergency SOS alerts from family members"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_LOCATION, "Location Sharing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Family location tracking status"
            setShowBadge(false)
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_WALKIE, "Walkie-Talkie",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "PTT walkie-talkie status"
            setShowBadge(false)
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_GENERAL, "General",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tasks, calendar reminders, and updates"
        })

        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_DRIVE, "Drive Sync",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Google Drive backup status"
            setShowBadge(false)
        })
    }

    // ── Chat Message Notification ─────────────────────────────────────────────
    fun showChatNotification(
        context: Context,
        senderName: String,
        preview: String,    // Never show decrypted content here — show "New message" instead
        senderId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("nav_to", "chat")
            putExtra("member_id", senderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, senderId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_nav_chat)
            .setContentTitle(senderName)
            .setContentText(preview)    // Show only "New encrypted message" for security
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        NotificationManagerCompat.from(context).notify(notifIdCounter++, notification)
    }

    // ── SOS Notification ─────────────────────────────────────────────────────
    fun showSOSNotification(
        context: Context,
        memberName: String,
        lat: Double,
        lng: Double
    ) {
        val mapsUri = "geo:$lat,$lng?q=$lat,$lng(${memberName}+SOS)"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("nav_to", "sos")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_sos_alert)
            .setContentTitle("🆘 SOS Alert — $memberName")
            .setContentText("$memberName needs help! Tap to view location.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$memberName has triggered Emergency SOS.\nLocation: $lat, $lng\n\nTap to open map."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(0xFFFF3D71.toInt())
            .build()
        NotificationManagerCompat.from(context).notify(9999, notification)
    }

    // ── Drive Sync Notification ───────────────────────────────────────────────
    fun showDriveSyncNotification(context: Context, status: String, isError: Boolean = false) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DRIVE)
            .setSmallIcon(if (isError) R.drawable.ic_sos_alert else R.drawable.ic_upload)
            .setContentTitle("Family4 Drive Sync")
            .setContentText(status)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        NotificationManagerCompat.from(context).notify(2000, notification)
    }

    // ── General Reminder Notification ─────────────────────────────────────────
    fun showReminderNotification(
        context: Context,
        title: String,
        body: String,
        id: Int = notifIdCounter++
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    // ── In-App Toast Helper ───────────────────────────────────────────────────
    /**
     * Shows a styled in-app toast. Call from main thread.
     * For background threads use Handler(Looper.getMainLooper()).post { showToast(...) }
     */
    fun showToast(context: Context, message: String, isError: Boolean = false) {
        android.widget.Toast.makeText(
            context,
            if (isError) "⚠️ $message" else message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun showLongToast(context: Context, message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }
}
