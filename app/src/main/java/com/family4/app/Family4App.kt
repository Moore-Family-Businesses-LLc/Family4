package com.family4.app

import android.app.Application
import com.family4.app.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Family4App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Register all notification channels at app startup
        NotificationHelper.createAllChannels(this)
    }

    companion object {
        lateinit var instance: Family4App
            private set
    }
}
