package com.family4.app.data.db.dao

import androidx.room.*
import com.family4.app.data.db.entity.*
import kotlinx.coroutines.flow.Flow

// ── Screen-time DAO ────────────────────────────────────────────────────────
@Dao
interface ScreenTimeDao {
    @Query("SELECT * FROM screen_time WHERE memberId = :id ORDER BY date DESC LIMIT 30")
    fun getScreenTimeHistory(id: String): Flow<List<ScreenTimeEntity>>

    @Query("SELECT * FROM screen_time WHERE memberId = :id AND date = :date LIMIT 1")
    suspend fun getForDay(id: String, date: Long): ScreenTimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ScreenTimeEntity)

    @Query("SELECT * FROM screen_time WHERE date = :date ORDER BY totalMinutes DESC")
    fun getAllForDay(date: Long): Flow<List<ScreenTimeEntity>>
}

// ── Kid-event DAO ──────────────────────────────────────────────────────────
@Dao
interface KidEventDao {
    @Insert
    suspend fun insert(event: KidEventEntity)

    @Query("SELECT * FROM kid_events WHERE memberId = :id ORDER BY timestamp DESC LIMIT 100")
    fun getRecentEvents(id: String): Flow<List<KidEventEntity>>

    @Query("SELECT COUNT(*) FROM kid_events WHERE memberId = :id AND eventType = :type AND timestamp >= :since")
    suspend fun countSince(id: String, type: String, since: Long): Int

    @Query("DELETE FROM kid_events WHERE timestamp < :before")
    suspend fun pruneOld(before: Long)
}

// ── Safe-zone DAO ──────────────────────────────────────────────────────────
@Dao
interface SafeZoneDao {
    @Query("SELECT * FROM safe_zones ORDER BY createdAt DESC")
    fun getAllZones(): Flow<List<SafeZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zone: SafeZoneEntity): Long

    @Update
    suspend fun update(zone: SafeZoneEntity)

    @Delete
    suspend fun delete(zone: SafeZoneEntity)
}

// ── Chore DAO ──────────────────────────────────────────────────────────────
@Dao
interface ChoreDao {
    @Query("SELECT * FROM chores WHERE isCompleted = 0 ORDER BY dueDate ASC, pointValue DESC")
    fun getActiveChores(): Flow<List<ChoreEntity>>

    @Query("SELECT * FROM chores WHERE assignedTo = :memberId ORDER BY isCompleted ASC, dueDate ASC")
    fun getChoresFor(memberId: String): Flow<List<ChoreEntity>>

    @Query("SELECT SUM(pointValue) FROM chores WHERE assignedTo = :memberId AND isCompleted = 1")
    fun getTotalPoints(memberId: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chore: ChoreEntity): Long

    @Update
    suspend fun update(chore: ChoreEntity)

    @Delete
    suspend fun delete(chore: ChoreEntity)

    @Query("UPDATE chores SET isCompleted = 1, completedAt = :at WHERE id = :id")
    suspend fun complete(id: Long, at: Long = System.currentTimeMillis())
}

// ── Poll DAO ───────────────────────────────────────────────────────────────
@Dao
interface PollDao {
    @Query("SELECT * FROM polls WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActivePolls(): Flow<List<PollEntity>>

    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun getAllPolls(): Flow<List<PollEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poll: PollEntity): Long

    @Update
    suspend fun update(poll: PollEntity)

    @Delete
    suspend fun delete(poll: PollEntity)
}

// ── Shopping DAO ───────────────────────────────────────────────────────────
@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isChecked ASC, category ASC, name ASC")
    fun getAll(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun update(item: ShoppingItemEntity)

    @Delete
    suspend fun delete(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isChecked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun clearChecked()
}

// ── Bedtime DAO ────────────────────────────────────────────────────────────
@Dao
interface BedtimeDao {
    @Query("SELECT * FROM bedtime_alerts WHERE isEnabled = 1")
    fun getActiveAlerts(): Flow<List<BedtimeAlertEntity>>

    @Query("SELECT * FROM bedtime_alerts WHERE memberId = :memberId LIMIT 1")
    suspend fun getForMember(memberId: String): BedtimeAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: BedtimeAlertEntity)

    @Delete
    suspend fun delete(alert: BedtimeAlertEntity)
}
