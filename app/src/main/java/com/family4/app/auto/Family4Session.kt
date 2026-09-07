package com.family4.app.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

/**
 * Auto session — starts the dashboard as the root screen.
 */
class Family4Session : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        DashboardScreen(carContext)
}
