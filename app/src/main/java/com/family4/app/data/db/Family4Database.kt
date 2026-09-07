package com.family4.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.family4.app.data.db.converters.Converters
import com.family4.app.data.db.dao.*
import com.family4.app.data.db.entity.*
import com.family4.app.vehicle.model.VehicleTripDao
import com.family4.app.vehicle.model.VehicleTripEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteTagEntity::class,
        ChatMessageEntity::class,
        FamilyMemberEntity::class,
        CalendarEventEntity::class,
        TaskEntity::class,
        LocationSnapshotEntity::class,
        HealthRecordEntity::class,
        PhotoAlbumEntity::class,
        PhotoEntity::class,
        BoardPostEntity::class,
        // ── v4 additions ──
        ScreenTimeEntity::class,
        KidEventEntity::class,
        SafeZoneEntity::class,
        ChoreEntity::class,
        PollEntity::class,
        ShoppingItemEntity::class,
        BedtimeAlertEntity::class,
        // ── v5 additions ──
        VehicleTripEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class Family4Database : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao
    abstract fun memberDao(): FamilyMemberDao
    abstract fun calendarDao(): CalendarDao
    abstract fun taskDao(): TaskDao
    abstract fun locationDao(): LocationDao
    abstract fun healthDao(): HealthDao
    abstract fun albumDao(): AlbumDao
    abstract fun boardDao(): BoardDao
    // ── v4 DAOs ──
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun kidEventDao(): KidEventDao
    abstract fun safeZoneDao(): SafeZoneDao
    abstract fun choreDao(): ChoreDao
    abstract fun pollDao(): PollDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun bedtimeDao(): BedtimeDao
    // ── v5 DAOs ──
    abstract fun vehicleTripDao(): VehicleTripDao
}
