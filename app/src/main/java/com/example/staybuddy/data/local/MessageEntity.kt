package com.example.staybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.staybuddy.data.model.Message

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val isRead: Boolean,
    val isDeleted: Boolean,
    val isSyncing: Boolean = false
)

fun MessageEntity.toDomainModel(): Message {
    return Message(
        messageId = messageId,
        roomId = roomId,
        senderId = senderId,
        text = text,
        timestamp = timestamp,
        isRead = isRead,
        isDeleted = isDeleted,
        isSyncing = isSyncing
    )
}

fun Message.toEntity(roomId: String): MessageEntity {
    return MessageEntity(
        messageId = messageId,
        roomId = roomId,
        senderId = senderId,
        text = text,
        timestamp = timestamp,
        isRead = isRead,
        isDeleted = isDeleted,
        isSyncing = isSyncing
    )
}
