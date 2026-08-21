package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val voiceName: String = "Puck",
    val audioFilePath: String? = null,
    val audioDurationMs: Long = 0L,
    val isSavedToStorage: Boolean = false,
    val storageUriString: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
