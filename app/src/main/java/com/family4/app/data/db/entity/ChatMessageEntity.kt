package com.family4.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,           // UUID
    val senderId: String,
    val receiverId: String,
    val encryptedContent: String,         // AES-256-GCM ciphertext (base64)
    val iv: String,                       // Initialisation vector (base64)
    val messageType: String = "TEXT",     // TEXT | IMAGE | FILE | VOICE | LOCATION
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isDelivered: Boolean = false,
    val isSentByMe: Boolean = false
)
