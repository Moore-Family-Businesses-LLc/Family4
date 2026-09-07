package com.family4.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String,           // UUID
    val displayName: String,
    val avatarUri: String? = null,
    val phoneNumber: String? = null,
    val role: String = "MEMBER",          // ADMIN | MEMBER | CHILD
    val publicKey: String = "",           // RSA-2048 public key (base64) for E2E key exchange
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationUpdatedAt: Long? = null,
    val batteryLevel: Int = -1,
    val statusMessage: String = ""
)
