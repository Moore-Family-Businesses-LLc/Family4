package com.family4.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A Family Board post — shared announcements, photos, reactions visible to all family members.
 * postType: "chat" | "announcement" | "event" | "photo" | "task"
 */
@Entity(tableName = "board_posts")
data class BoardPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorId: String,
    val authorName: String,
    val content: String,
    val imageUri: String = "",
    val pinned: Boolean = false,
    val reactions: String = "",      // JSON string: {"❤️":3,"👍":2}
    val postType: String = "chat",   // "chat" | "announcement" | "event" | "photo" | "task"
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null
)
