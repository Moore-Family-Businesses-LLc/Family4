package com.family4.app.data.db.dao

import androidx.room.*
import com.family4.app.data.db.entity.BoardPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {

    @Query("SELECT * FROM board_posts ORDER BY pinned DESC, createdAt DESC")
    fun getAllPosts(): Flow<List<BoardPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: BoardPostEntity): Long

    @Update
    suspend fun updatePost(post: BoardPostEntity)

    @Delete
    suspend fun deletePost(post: BoardPostEntity)

    @Query("UPDATE board_posts SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE board_posts SET reactions = :reactions WHERE id = :id")
    suspend fun updateReactions(id: Long, reactions: String)

    @Query("DELETE FROM board_posts WHERE createdAt < :before AND pinned = 0")
    suspend fun pruneOld(before: Long)
}
