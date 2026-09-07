package com.family4.app.data.db.dao

import androidx.room.*
import com.family4.app.data.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY displayName ASC")
    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: String): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity)

    @Update
    suspend fun updateMember(member: FamilyMemberEntity)

    @Delete
    suspend fun deleteMember(member: FamilyMemberEntity)

    @Query("UPDATE family_members SET latitude = :lat, longitude = :lng, locationUpdatedAt = :ts WHERE id = :id")
    suspend fun updateLocation(id: String, lat: Double, lng: Double, ts: Long)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startTime >= :from AND startTime <= :to ORDER BY startTime ASC")
    fun getEventsInRange(from: Long, to: Long): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :done, completedAt = :at WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean, at: Long?)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_snapshots WHERE memberId = :id ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocation(id: String): LocationSnapshotEntity?

    @Query("SELECT * FROM location_snapshots WHERE memberId = :id ORDER BY timestamp DESC LIMIT :limit")
    fun getLocationHistory(id: String, limit: Int = 50): Flow<List<LocationSnapshotEntity>>

    @Insert
    suspend fun insertSnapshot(snapshot: LocationSnapshotEntity)

    @Query("DELETE FROM location_snapshots WHERE timestamp < :before")
    suspend fun pruneOld(before: Long)
}

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_records WHERE memberId = :id ORDER BY recordDate DESC LIMIT 30")
    fun getRecentRecords(id: String): Flow<List<HealthRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: HealthRecordEntity)
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM photo_albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<PhotoAlbumEntity>>

    @Query("SELECT * FROM photo_albums ORDER BY createdAt DESC")
    suspend fun getAllAlbumsOnce(): List<PhotoAlbumEntity>

    @Query("SELECT * FROM photos WHERE albumId = :albumId ORDER BY takenAt DESC")
    fun getPhotosInAlbum(albumId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT COUNT(*) FROM photos WHERE albumId = :albumId")
    suspend fun getPhotoCount(albumId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: PhotoAlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Query("UPDATE photo_albums SET coverUri = :uri WHERE id = :albumId")
    suspend fun updateCover(albumId: Long, uri: String)

    @Delete
    suspend fun deleteAlbum(album: PhotoAlbumEntity)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)
}
