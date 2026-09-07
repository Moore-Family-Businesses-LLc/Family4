package com.family4.app.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.family4.app.R
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.dao.LocationDao
import com.family4.app.data.db.entity.LocationSnapshotEntity
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : LifecycleService() {

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var memberDao: FamilyMemberDao

    private lateinit var fusedClient: FusedLocationProviderClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                scope.launch {
                    // Save snapshot
                    locationDao.insertSnapshot(
                        LocationSnapshotEntity(
                            memberId = "self",   // Replace with real user ID from prefs
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            altitude = loc.altitude,
                            speed = loc.speed,
                            timestamp = loc.time
                        )
                    )
                    // Update member record
                    memberDao.updateLocation("self", loc.latitude, loc.longitude, loc.time)
                    // Prune old snapshots (keep last 7 days)
                    locationDao.pruneOld(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundNotification()
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30_000L     // every 30 seconds
        ).setMinUpdateIntervalMillis(15_000L)
         .setMaxUpdateDelayMillis(60_000L)
         .build()

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) { /* Permission not granted */ }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Family4 Location")
            .setContentText("Sharing location with family")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIF_ID = 1001
    }
}
