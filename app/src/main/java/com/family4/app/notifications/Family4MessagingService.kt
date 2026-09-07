package com.family4.app.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Family4MessagingService — handles all incoming Firebase Cloud Messaging push
 * notifications for Family4.
 *
 * Payload types (sent from your server or Firebase console):
 *   type=chat         → new encrypted message
 *   type=sos          → emergency SOS from a family member
 *   type=location     → location update ping
 *   type=drive_sync   → drive sync result
 *   type=reminder     → calendar/task reminder
 */
@AndroidEntryPoint
class Family4MessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        // TODO: Send this token to your backend so it can send targeted pushes
        // to this device. Store locally in DataStore for reference.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"] ?: "general"
        val notification = message.notification

        Log.d(TAG, "FCM received: type=$type, keys=${data.keys}")

        when (type) {
            "chat" -> {
                val senderName = data["sender_name"] ?: "Family member"
                val senderId   = data["sender_id"]   ?: ""
                // Never show actual message content in the notification for privacy
                NotificationHelper.showChatNotification(
                    context       = this,
                    senderName    = senderName,
                    preview       = "New encrypted message",
                    senderId      = senderId
                )
            }

            "sos" -> {
                val memberName = data["member_name"] ?: "Family member"
                val lat = data["lat"]?.toDoubleOrNull() ?: 0.0
                val lng = data["lng"]?.toDoubleOrNull() ?: 0.0
                NotificationHelper.showSOSNotification(
                    context    = this,
                    memberName = memberName,
                    lat        = lat,
                    lng        = lng
                )
            }

            "drive_sync" -> {
                val status = data["status"] ?: "Sync complete"
                val isError = data["is_error"]?.toBoolean() ?: false
                NotificationHelper.showDriveSyncNotification(this, status, isError)
            }

            "reminder" -> {
                val title = data["title"]   ?: notification?.title ?: "Reminder"
                val body  = data["body"]    ?: notification?.body  ?: ""
                NotificationHelper.showReminderNotification(this, title, body)
            }

            else -> {
                // Generic notification using Firebase Notification payload
                if (notification != null) {
                    NotificationHelper.showReminderNotification(
                        this,
                        notification.title ?: "Family4",
                        notification.body  ?: ""
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "Family4FCM"
    }
}
