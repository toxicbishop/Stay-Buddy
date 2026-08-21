package com.example.staybuddy.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.ChatRoom
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val chatRooms: List<ChatRoom> = emptyList(),
    val userNames: Map<String, String> = emptyMap(),
    val userAvatars: Map<String, String> = emptyMap(),
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadChatRooms()
    }

    private fun loadChatRooms() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "User not authenticated"
            )
            return
        }

        _uiState.value = _uiState.value.copy(currentUserId = userId)

        viewModelScope.launch {
            chatRepository.getUserChatRooms(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load chats"
                    )
                }
                .collect { rooms ->
                    val visibleRooms = rooms.filter { !it.deletedBy.contains(userId) }
                        .distinctBy { room -> room.participants.sorted().joinToString() + room.roommatePostId }
                    
                    val namesMap = _uiState.value.userNames.toMutableMap()
                    val avatarsMap = _uiState.value.userAvatars.toMutableMap()
                    
                    // 1. Instant local load to prevent flicker
                    visibleRooms.forEach { room ->
                        val otherId = room.participants.firstOrNull { it != userId } ?: return@forEach
                        val localUser = authRepository.getLocalUser(otherId)
                        if (localUser != null) {
                            namesMap[otherId] = localUser.name.ifBlank { "Unknown User" }
                            if (localUser.profileImage.isNotEmpty()) {
                                avatarsMap[otherId] = localUser.profileImage
                            }
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        chatRooms = visibleRooms,
                        userNames = namesMap,
                        userAvatars = avatarsMap,
                        error = null
                    )
                    
                    // 2. Async fetch to update any stale profiles
                    fetchUserNamesAsync(visibleRooms, userId)
                }
        }
    }

    private fun fetchUserNamesAsync(rooms: List<ChatRoom>, currentUserId: String) {
        val otherUserIds = rooms.mapNotNull { room ->
            room.participants.firstOrNull { it != currentUserId }
        }.distinct()

        otherUserIds.forEach { id ->
            viewModelScope.launch {
                authRepository.getUserFromFirestore(id)
                    .onSuccess { user ->
                        val currentNames = _uiState.value.userNames.toMutableMap()
                        val currentAvatars = _uiState.value.userAvatars.toMutableMap()
                        
                        if (user != null) {
                            currentNames[id] = user.name.ifBlank { "Unknown User" }
                            if (user.profileImage.isNotEmpty()) {
                                currentAvatars[id] = user.profileImage
                            }
                        } else {
                            currentNames[id] = "Unknown User"
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            userNames = currentNames,
                            userAvatars = currentAvatars
                        )
                    }
                    .onFailure {
                        val currentNames = _uiState.value.userNames.toMutableMap()
                        currentNames[id] = "Unknown User"
                        _uiState.value = _uiState.value.copy(
                            userNames = currentNames
                        )
                    }
            }
        }
    }

    fun markAsRead(roomId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(roomId, userId)
        }
    }

    fun clearChat(roomId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.clearChat(roomId, userId)
        }
    }

    fun deleteChat(roomId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.deleteChat(roomId, userId)
        }
    }
}
