package com.family4.app.vehicle.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleTripDao {

    @Query("SELECT * FROM vehicle_trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<VehicleTripEntity>>

    @Query("SELECT * FROM vehicle_trips ORDER BY startTime DESC LIMIT :limit")
    fun getRecentTrips(limit: Int = 20): Flow<List<VehicleTripEntity>>

    @Query("SELECT * FROM vehicle_trips WHERE startTime >= :from ORDER BY startTime DESC")
    fun getTripsFrom(from: Long): Flow<List<VehicleTripEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: VehicleTripEntity): Long

    @Update
    suspend fun updateTrip(trip: VehicleTripEntity)

    @Delete
    suspend fun deleteTrip(trip: VehicleTripEntity)

    @Query("DELETE FROM vehicle_trips WHERE startTime < :before")
    suspend fun pruneOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM vehicle_trips")
    suspend fun count(): Int

    @Query("SELECT SUM(distanceKm) FROM vehicle_trips")
    suspend fun totalDistanceKm(): Double?
}
