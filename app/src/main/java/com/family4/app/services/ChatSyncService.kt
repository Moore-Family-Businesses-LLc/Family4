package com.family4.app.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * ChatSyncService — background service for keeping encrypted chat
 * messages in sync across family members via FCM.
 * Currently a stub; full implementation uses FCM data messages.
 */
class ChatSyncService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
