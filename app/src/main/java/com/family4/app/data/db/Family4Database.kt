package com.family4.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.family4.app.data.db.converters.Converters
import com.family4.app.data.db.dao.*
import com.family4.app.data.db.entity.*

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
        BoardPostEntity::class
    ],
    version = 2,
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
}
