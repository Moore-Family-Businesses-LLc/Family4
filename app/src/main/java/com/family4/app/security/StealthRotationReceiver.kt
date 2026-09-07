package com.family4.app.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires every 14 days to remind the parent to rotate the stealth pattern. */
class StealthRotationReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        StealthModeManager.sendRotationNotification(ctx)
    }
}
