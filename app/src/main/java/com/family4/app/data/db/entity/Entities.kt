package com.family4.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean = false,
    val location: String = "",
    val color: Int = 0xFF3B82D4.toInt(),
    val isRecurring: Boolean = false,
    val recurrenceRule: String = "",  // RRULE string
    val reminderMinutes: Int = 15,
    val createdBy: String = "",       // memberId
    val sharedWithAll: Boolean = true
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val priority: Int = 1,            // 0=Low 1=Normal 2=High 3=Urgent
    val assignedTo: String = "",      // memberId
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val listName: String = "General"
)

@Entity(tableName = "location_snapshots")
data class LocationSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val address: String = ""
)

@Entity(tableName = "health_records")
data class HealthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: String,
    val steps: Int = 0,
    val heartRate: Int = 0,
    val calories: Int = 0,
    val sleepHours: Float = 0f,
    val waterIntakeMl: Int = 0,
    val recordDate: Long = System.currentTimeMillis(),
    val weightKg: Float = 0f,
    val moodScore: Int = 3           // 1-5 scale
)

@Entity(tableName = "photo_albums")
data class PhotoAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isShared: Boolean = true,
    val createdBy: String = ""
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long,
    val uri: String,
    val caption: String = "",
    val takenAt: Long = System.currentTimeMillis(),
    val takenBy: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Entity(tableName = "note_tags")
data class NoteTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val tag: String
)
