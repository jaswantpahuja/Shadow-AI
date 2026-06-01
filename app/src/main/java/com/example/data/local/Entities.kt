package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val sender: String, // "user" or "ai"
    val text: String,
    val imagePath: String? = null, // Local absolute path to the saved PNG image
    val aspectRatio: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
