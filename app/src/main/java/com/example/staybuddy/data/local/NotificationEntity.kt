package com.example.staybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val routeType: String, // e.g., "chat", "inquiry", "system"
    val routeId: String,   // e.g., roomId or inquiryId
    val avatarUrl: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
