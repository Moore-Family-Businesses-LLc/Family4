package com.family4.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.family4.app.services.LocationTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Restart location tracking after device boot
            context.startForegroundService(
                Intent(context, LocationTrackingService::class.java)
            )
        }
    }
}
