package com.family4.app.vehicle.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged drive trip — recorded from OBD session start to session end.
 * Written to Room so trips persist across restarts.
 */
@Entity(tableName = "vehicle_trips")
data class VehicleTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long  = System.currentTimeMillis(),
    val endTime: Long?   = null,
    val distanceKm: Double = 0.0,
    val durationSec: Long  = 0L,
    val maxSpeedKph: Double = 0.0,
    val avgSpeedKph: Double = 0.0,
    val maxRpm: Double      = 0.0,
    val maxCoolantC: Double = 0.0,
    val startFuelPct: Double? = null,
    val endFuelPct: Double?   = null,
    val fuelUsedL: Double?    = null,
    val dtcCount: Int         = 0,
    val notes: String         = ""
)
