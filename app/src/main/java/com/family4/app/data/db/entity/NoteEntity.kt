package com.family4.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val color: Int = 0xFFFFFFFF.toInt(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistJson: String = "[]",   // JSON array of ChecklistItem
    val imageUri: String? = null,
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val label: String? = null
)
