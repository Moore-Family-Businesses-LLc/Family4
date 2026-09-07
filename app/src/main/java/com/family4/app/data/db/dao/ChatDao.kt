package com.family4.app.data.db.dao

import androidx.room.*
import com.family4.app.data.db.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE (senderId = :me AND receiverId = :other) OR (senderId = :other AND receiverId = :me) ORDER BY timestamp ASC")
    fun getConversation(me: String, other: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE receiverId = :me AND isRead = 0")
    fun getUnreadMessages(me: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :me AND senderId = :from AND isRead = 0")
    fun getUnreadCountFrom(me: String, from: String): Flow<Int>

    /** Total unread across every conversation — drives the bottom-nav badge. */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :me AND isRead = 0")
    fun getUnreadCount(me: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE receiverId = :me AND senderId = :from")
    suspend fun markAllRead(me: String, from: String)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    /** Wipes every message — backs "Clear chat history" in Settings. */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    /** Retention sweep: drops messages older than [before] (epoch millis). */
    @Query("DELETE FROM chat_messages WHERE timestamp < :before")
    suspend fun deleteMessagesOlderThan(before: Long)

    @Query("SELECT DISTINCT CASE WHEN senderId = :me THEN receiverId ELSE senderId END AS partnerId FROM chat_messages WHERE senderId = :me OR receiverId = :me")
    fun getConversationPartners(me: String): Flow<List<String>>
}
