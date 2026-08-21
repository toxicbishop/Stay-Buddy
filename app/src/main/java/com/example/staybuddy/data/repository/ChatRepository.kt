package com.example.staybuddy.data.repository

import com.example.staybuddy.data.model.ChatRoom
import com.example.staybuddy.data.model.Message
import com.example.staybuddy.data.model.RoommatePost
import com.example.staybuddy.data.model.User
import com.example.staybuddy.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import com.example.staybuddy.data.api.NotificationApi
import com.example.staybuddy.data.local.ChatDao
import com.example.staybuddy.data.local.toEntity
import com.example.staybuddy.data.local.toDomainModel

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatDao: ChatDao,
    private val notificationApi: NotificationApi
) {
    fun getUserChatRooms(userId: String): Flow<List<ChatRoom>> = channelFlow {
        launch(Dispatchers.IO) {
            chatDao.getAllChatRooms().collect { list ->
                send(list.map { it.toDomainModel() }.filter { it.participants.contains(userId) })
            }
        }
        
        val listener = firestore.collection(Constants.CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val changedRooms = snapshot.documentChanges.mapNotNull { it.document.toObject(ChatRoom::class.java) }
                    if (changedRooms.isNotEmpty()) {
                        launch(Dispatchers.IO) {
                            chatDao.insertChatRooms(changedRooms.map { it.toEntity() })
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun getChatRoom(chatId: String): Result<ChatRoom?> {
        return try {
            val doc = firestore.collection(Constants.CHATS_COLLECTION).document(chatId).get().await()
            Result.success(doc.toObject(ChatRoom::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatRoomFlow(chatId: String): Flow<ChatRoom?> = channelFlow {
        launch(Dispatchers.IO) {
            chatDao.getChatRoom(chatId).collect { entity ->
                send(entity?.toDomainModel())
            }
        }
        
        val listener = firestore.collection(Constants.CHATS_COLLECTION)
            .document(chatId)
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    val room = snapshot?.toObject(ChatRoom::class.java)
                    if (room != null) {
                        launch(Dispatchers.IO) {
                            chatDao.insertChatRoom(room.toEntity())
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getOrCreateChatRoom(
        user1Id: String, 
        user2Id: String, 
        listingId: String? = null,
        roommatePostId: String? = null
    ): Result<String> {
        return try {
            val existingRooms = firestore.collection(Constants.CHATS_COLLECTION)
                .whereArrayContains("participants", user1Id)
                .get()
                .await()
                .toObjects(ChatRoom::class.java)
                
            // Find ANY existing chat between these two users (1-to-1 chat model)
            val room = existingRooms.find { it.participants.contains(user2Id) }
            
            if (room != null) {
                // Optional: If we want to update the latest listing/post context, we could do it here
                Result.success(room.roomId)
            } else {
                val sortedParticipants = listOf(user1Id, user2Id).sorted()
                // Strict 1-to-1 Chat ID
                val deterministicId = "${sortedParticipants[0]}_${sortedParticipants[1]}"
                val docRef = firestore.collection(Constants.CHATS_COLLECTION).document(deterministicId)
                
                val newRoom = ChatRoom(
                    roomId = docRef.id,
                    participants = listOf(user1Id, user2Id),
                    listingId = listingId ?: "",
                    roommatePostId = roommatePostId ?: "",
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis()
                )
                docRef.set(newRoom, com.google.firebase.firestore.SetOptions.merge()).await()
                Result.success(docRef.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMessagesForRoom(roomId: String): Flow<List<Message>> = channelFlow {
        launch(Dispatchers.IO) {
            chatDao.getMessagesForRoom(roomId).collect { list ->
                send(list.map { it.toDomainModel() })
            }
        }
        
        val listener = firestore.collection(Constants.CHATS_COLLECTION)
            .document(roomId)
            .collection(Constants.MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val changedMessages = snapshot.documentChanges.mapNotNull { it.document.toObject(Message::class.java) }
                    if (changedMessages.isNotEmpty()) {
                        launch(Dispatchers.IO) {
                            chatDao.insertMessages(changedMessages.map { it.toEntity(roomId) })
                        }
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun confirmMatch(chatId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val roomRef = firestore.collection(Constants.CHATS_COLLECTION).document(chatId)
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                    ?: throw Exception("Chat not found")
                
                if (!room.confirmedBy.contains(userId)) {
                    val updatedConfirmedBy = room.confirmedBy + userId
                    val isConfirmed = updatedConfirmedBy.size >= 2
                    
                    transaction.update(roomRef, "confirmedBy", updatedConfirmedBy)
                    if (isConfirmed) {
                        transaction.update(roomRef, "isMatchConfirmed", true)
                        
                        // If it's a roommate match, decrement beds
                        if (room.roommatePostId.isNotEmpty()) {
                            val postRef = firestore.collection(Constants.ROOMMATE_POSTS_COLLECTION)
                                .document(room.roommatePostId)
                            val post = transaction.get(postRef).toObject(RoommatePost::class.java)
                            if (post != null && post.availableBeds > 0) {
                                val newAvailable = post.availableBeds - 1
                                transaction.update(postRef, "availableBeds", newAvailable)
                                if (newAvailable == 0) {
                                    transaction.update(postRef, "isActive", false)
                                }
                            }
                        }
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(roomId: String, userId: String): Result<Unit> {
        return try {
            val roomRef = firestore.collection(Constants.CHATS_COLLECTION).document(roomId)
            val roomSnapshot = roomRef.get().await()
            val room = roomSnapshot.toObject(ChatRoom::class.java) ?: throw Exception("Chat not found")

            // Reset unread count for this user
            val updatedUnreadCount = room.unreadCount.toMutableMap()
            updatedUnreadCount[userId] = 0
            
            roomRef.update("unreadCount", updatedUnreadCount).await()
            
            // Optimistic local update
            chatDao.markMessagesAsRead(roomId, userId)
            
            // Separately update individual messages to avoid heavy transaction
            val unreadMessages = firestore.collection(Constants.CHATS_COLLECTION)
                .document(roomId)
                .collection(Constants.MESSAGES_COLLECTION)
                .whereEqualTo("isRead", false)
                .get()
                .await()
                
            val batch = firestore.batch()
            for (doc in unreadMessages) {
                if (doc.getString("senderId") != userId) {
                    batch.update(doc.reference, "isRead", true)
                }
            }
            batch.commit().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(roomId: String, senderId: String, text: String): Result<Unit> {
        val messageRef = firestore.collection(Constants.CHATS_COLLECTION)
            .document(roomId)
            .collection(Constants.MESSAGES_COLLECTION)
            .document()
            
        return try {
                
            val message = Message(
                messageId = messageRef.id,
                roomId = roomId,
                senderId = senderId,
                text = text,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                isSyncing = true
            )
            
            // Optimistic update: write to local database immediately
            chatDao.insertMessage(message.toEntity(roomId))
            
            // Use WriteBatch instead of transaction for latency compensation and instant UI updates
            val batch = firestore.batch()
            batch.set(messageRef, message)
            
            val roomRef = firestore.collection(Constants.CHATS_COLLECTION).document(roomId)
            
            // Get room data (optimistically from local DB first, fallback to network)
            var room = chatDao.getChatRoom(roomId).firstOrNull()?.toDomainModel()
            if (room == null) {
                room = roomRef.get().await().toObject(ChatRoom::class.java) ?: throw Exception("Room not found")
            }
            
            val updates = mutableMapOf<String, Any>(
                "lastMessage" to text,
                "lastMessageTime" to message.timestamp,
                "deletedBy" to emptyList<String>()
            )
            
            val updatedUnreadCount = room.unreadCount.toMutableMap()
            val receivers = room.participants.filter { it != senderId }
            receivers.forEach { participantId ->
                updatedUnreadCount[participantId] = (updatedUnreadCount[participantId] ?: 0) + 1
            }
            updates["unreadCount"] = updatedUnreadCount
            
            batch.update(roomRef, updates)
            batch.commit().await()
            
            // Fire-and-forget push notification to other participants
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    val roomDoc = firestore.collection(Constants.CHATS_COLLECTION).document(roomId).get().await()
                    val room = roomDoc.toObject(ChatRoom::class.java)
                    if (room != null) {
                        // Re-check block status since we can't easily pass it from the transaction,
                        // or we just fetch it again to be safe.
                        val otherParticipants = room.participants.filter { it != senderId }
                        val senderDoc = firestore.collection(Constants.USERS_COLLECTION).document(senderId).get().await()
                        val senderName = senderDoc.getString("name") ?: "New Message"
                        
                        otherParticipants.forEach { targetId ->
                            val targetDoc = firestore.collection(Constants.USERS_COLLECTION).document(targetId).get().await()
                            val targetUser = targetDoc.toObject(User::class.java)
                            if (targetUser?.blockedUsers?.contains(senderId) != true) {
                                notificationApi.sendNotification(
                                    com.example.staybuddy.data.api.NotificationRequest(
                                        targetUserId = targetId,
                                        title = senderName,
                                        body = text,
                                        routeType = "chat",
                                        routeId = roomId,
                                        avatarUrl = senderDoc.getString("profileImage"),
                                        messageId = message.messageId,
                                        senderId = message.senderId
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore push notification failure, message was already sent
                    e.printStackTrace()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            // Revert optimistic update on failure by completely removing it
            chatDao.deleteMessageLocally(messageRef.id)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(roomId: String, messageId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.CHATS_COLLECTION)
                .document(roomId)
                .collection(Constants.MESSAGES_COLLECTION)
                .document(messageId)
                .update("isDeleted", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(roomId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val roomRef = firestore.collection(Constants.CHATS_COLLECTION).document(roomId)
            if (isTyping) {
                roomRef.update("typingStatus.$userId", System.currentTimeMillis()).await()
            } else {
                roomRef.update("typingStatus.$userId", com.google.firebase.firestore.FieldValue.delete()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUnreadChatsCount(userId: String): Flow<Int> = channelFlow {
        launch(Dispatchers.IO) {
            chatDao.getAllChatRooms().collect { list ->
                val count = list.count { 
                    (it.unreadCount[userId] ?: 0) > 0 && 
                    it.participants.contains(userId) && 
                    !it.deletedBy.contains(userId)
                }
                send(count)
            }
        }
        
        val listener = firestore.collection(Constants.CHATS_COLLECTION)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error == null) {
                    val rooms = snapshot?.toObjects(ChatRoom::class.java) ?: emptyList()
                    launch(Dispatchers.IO) {
                        chatDao.insertChatRooms(rooms.map { it.toEntity() })
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun clearChat(roomId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val roomRef = firestore.collection(Constants.CHATS_COLLECTION).document(roomId)
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                    ?: throw Exception("Room not found")
                
                val updatedClearedAt = room.clearedAt.toMutableMap()
                updatedClearedAt[userId] = System.currentTimeMillis()
                
                transaction.update(roomRef, "clearedAt", updatedClearedAt)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChat(roomId: String, userId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val roomRef = firestore.collection(Constants.CHATS_COLLECTION).document(roomId)
                val room = transaction.get(roomRef).toObject(ChatRoom::class.java)
                    ?: throw Exception("Room not found")
                
                val updatedDeletedBy = room.deletedBy.toMutableList()
                if (!updatedDeletedBy.contains(userId)) {
                    updatedDeletedBy.add(userId)
                }
                
                val updatedClearedAt = room.clearedAt.toMutableMap()
                updatedClearedAt[userId] = System.currentTimeMillis()
                
                transaction.update(roomRef, mapOf(
                    "deletedBy" to updatedDeletedBy,
                    "clearedAt" to updatedClearedAt
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
