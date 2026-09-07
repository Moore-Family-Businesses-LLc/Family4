package com.family4.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Screen-time record (one row per child per day) ─────────────────────────
@Entity(tableName = "screen_time")
data class ScreenTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: String,
    val date: Long,                         // epoch-millis for midnight of the day
    val totalMinutes: Int = 0,
    val limitMinutes: Int = 120,            // parent-set daily cap
    val appBreakdown: String = "",          // JSON: {"YouTube":30,"Games":45}
    val updatedAt: Long = System.currentTimeMillis()
)

// ── Kid event log (messages sent, calls, app-opens) ────────────────────────
@Entity(tableName = "kid_events")
data class KidEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: String,
    val eventType: String,                  // MSG_SENT | CALL_STARTED | CALL_ENDED | APP_OPENED
    val detail: String = "",               // "to:mom", "app:YouTube", etc.
    val timestamp: Long = System.currentTimeMillis()
)

// ── Geofence / Safe-zone definition ───────────────────────────────────────
@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 200f,
    val color: Int = 0xFF00D4FF.toInt(),
    val alertOnExit: Boolean = true,
    val alertOnEntry: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ── Chore entity ───────────────────────────────────────────────────────────
@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val assignedTo: String,                // memberId
    val pointValue: Int = 10,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val recurrence: String = "NONE",       // NONE|DAILY|WEEKLY
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val iconEmoji: String = "🧹"
)

// ── Family poll ────────────────────────────────────────────────────────────
@Entity(tableName = "polls")
data class PollEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val optionsJson: String,               // JSON array of option strings
    val votesJson: String = "{}",          // JSON: {"option":"memberId,memberId"}
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isActive: Boolean = true
)

// ── Shopping list item ─────────────────────────────────────────────────────
@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String = "1",
    val category: String = "General",
    val addedBy: String = "",
    val isChecked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

// ── Bedtime alert / curfew ─────────────────────────────────────────────────
@Entity(tableName = "bedtime_alerts")
data class BedtimeAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: String,
    val bedtimeHour: Int = 21,             // 24-hour
    val bedtimeMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // 1=Mon … 7=Sun
    val isEnabled: Boolean = true,
    val graceMinutes: Int = 15             // minutes before alert fires
)
