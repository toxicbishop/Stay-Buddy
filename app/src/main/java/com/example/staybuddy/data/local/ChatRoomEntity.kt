package com.example.staybuddy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.staybuddy.data.model.ChatRoom

@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey
    val roomId: String,
    val participants: List<String>,
    val listingId: String,
    val roommatePostId: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val confirmedBy: List<String>,
    val isMatchConfirmed: Boolean,
    val unreadCount: Map<String, Int>,
    val typingStatus: Map<String, Long>,
    val clearedAt: Map<String, Long>,
    val deletedBy: List<String>
)

fun ChatRoomEntity.toDomainModel(): ChatRoom {
    return ChatRoom(
        roomId = roomId,
        participants = participants,
        listingId = listingId,
        roommatePostId = roommatePostId,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        confirmedBy = confirmedBy,
        isMatchConfirmed = isMatchConfirmed,
        unreadCount = unreadCount,
        typingStatus = typingStatus,
        clearedAt = clearedAt,
        deletedBy = deletedBy
    )
}

fun ChatRoom.toEntity(): ChatRoomEntity {
    return ChatRoomEntity(
        roomId = roomId,
        participants = participants,
        listingId = listingId,
        roommatePostId = roommatePostId,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        confirmedBy = confirmedBy,
        isMatchConfirmed = isMatchConfirmed,
        unreadCount = unreadCount,
        typingStatus = typingStatus,
        clearedAt = clearedAt,
        deletedBy = deletedBy
    )
}
