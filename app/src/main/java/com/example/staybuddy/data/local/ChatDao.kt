package com.example.staybuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    // Chat Rooms
    @Query("SELECT * FROM chat_rooms ORDER BY lastMessageTime DESC")
    fun getAllChatRooms(): Flow<List<ChatRoomEntity>>

    @Query("SELECT * FROM chat_rooms WHERE roomId = :roomId")
    fun getChatRoom(roomId: String): Flow<ChatRoomEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRooms(rooms: List<ChatRoomEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(room: ChatRoomEntity)

    // Messages
    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isRead = 1 WHERE roomId = :roomId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(roomId: String, currentUserId: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE messageId = :messageId")
    suspend fun markMessageAsDeleted(messageId: String)
    
    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageLocally(messageId: String)
    
    @Query("DELETE FROM chat_rooms")
    suspend fun clearChatRooms()

    @Query("DELETE FROM messages")
    suspend fun clearMessages()
}
