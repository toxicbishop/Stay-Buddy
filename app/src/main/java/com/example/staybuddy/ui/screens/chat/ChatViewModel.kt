package com.example.staybuddy.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.Message
import com.example.staybuddy.data.model.RoommatePost
import com.example.staybuddy.data.repository.ChatRepository
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.RoommateRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val otherUserName: String = "",
    val otherUserAvatarUrl: String = "",
    val isMatchConfirmed: Boolean = false,
    val confirmedBy: List<String> = emptyList(),
    val roommatePostId: String = "",
    val roommatePost: RoommatePost? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSending: Boolean = false,
    val isOtherUserTyping: Boolean = false,
    val isBlocked: Boolean = false,
    val isBlockedByOther: Boolean = false,
    val otherUserId: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val roommateRepository: RoommateRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var typingJob: kotlinx.coroutines.Job? = null

    private val _allMessages = MutableStateFlow<List<Message>>(emptyList())
    private val _clearedAt = MutableStateFlow<Long>(0L)

    // Guard flags — prevent duplicate listener launches
    private var blockListenerJob: kotlinx.coroutines.Job? = null
    private var blockedByOtherListenerJob: kotlinx.coroutines.Job? = null

    init {
        loadRoomDetails()
        loadMessages()

        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(_allMessages, _clearedAt) { msgs, clearedTime ->
                msgs.filter { it.timestamp > clearedTime }
            }.collect { visibleMsgs ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    messages = visibleMsgs,
                    error = null
                )
                if (visibleMsgs.isNotEmpty()) {
                    markAsRead()
                }
            }
        }
    }

    private fun loadRoomDetails() {
        viewModelScope.launch {
            chatRepository.getChatRoomFlow(chatId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
                .collect { room ->
                    if (room != null) {
                        val currentUserId = auth.currentUser?.uid ?: ""
                        val otherUserId = room.participants.firstOrNull { it != currentUserId } ?: ""
                        
                        _clearedAt.value = room.clearedAt[currentUserId] ?: 0L
                        
                        // Derive the match flag from confirmedBy: it's the real source of
                        // truth and is immune to the legacy "matchConfirmed" field name
                        // drift that left old rooms showing as unconfirmed.
                        _uiState.value = _uiState.value.copy(
                            isMatchConfirmed = room.confirmedBy.size >= 2 || room.isMatchConfirmed,
                            confirmedBy = room.confirmedBy,
                            roommatePostId = room.roommatePostId,
                            otherUserId = otherUserId
                        )
                        fetchPostDetails(room.roommatePostId)
                        
                        val otherTypingTime = room.typingStatus[otherUserId] ?: 0L
                        val isTyping = (System.currentTimeMillis() - otherTypingTime) < 5000
                        _uiState.value = _uiState.value.copy(isOtherUserTyping = isTyping)
                        
                        if (otherUserId.isNotBlank() && _uiState.value.otherUserName.isBlank()) {
                            authRepository.getUserFromFirestore(otherUserId).onSuccess { user ->
                                if (user != null) {
                                    _uiState.value = _uiState.value.copy(
                                        otherUserName = user.name,
                                        otherUserAvatarUrl = user.profileImage
                                    )
                                }
                            }
                        }
                        
                        // Launch block status listeners ONCE (not on every typing event)
                        if (currentUserId.isNotBlank() && blockListenerJob == null) {
                            blockListenerJob = viewModelScope.launch {
                                authRepository.getUserFlow(currentUserId).collect { user ->
                                    if (user != null) {
                                        val blocked = user.blockedUsers.contains(otherUserId)
                                        _uiState.value = _uiState.value.copy(isBlocked = blocked)
                                    }
                                }
                            }
                        }
                        
                        if (otherUserId.isNotBlank() && currentUserId.isNotBlank() && blockedByOtherListenerJob == null) {
                            blockedByOtherListenerJob = viewModelScope.launch {
                                authRepository.getUserFlow(otherUserId).collect { user ->
                                    if (user != null) {
                                        val blockedByOther = user.blockedUsers.contains(currentUserId)
                                        _uiState.value = _uiState.value.copy(isBlockedByOther = blockedByOther)
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun loadMessages() {
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
            chatRepository.getMessagesForRoom(chatId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
                .collect { messages ->
                    _allMessages.value = messages
                }
        }
    }

    private fun markAsRead() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chatId, userId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userId = auth.currentUser?.uid ?: return
        
        _uiState.value = _uiState.value.copy(isSending = true)
        
        viewModelScope.launch {
            val result = chatRepository.sendMessage(chatId, userId, text)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSending = false, error = null)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "Failed to send message"
                )
            }
        }
    }

    fun confirmMatch() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            val result = chatRepository.confirmMatch(chatId, userId)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to confirm match")
            }
        }
    }

    private fun fetchPostDetails(postId: String) {
        if (postId.isBlank()) return
        viewModelScope.launch {
            roommateRepository.getRoommatePostById(postId).onSuccess { post ->
                _uiState.value = _uiState.value.copy(roommatePost = post)
            }
        }
    }

    fun onTyping() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.setTypingStatus(chatId, userId, true)
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            chatRepository.setTypingStatus(chatId, userId, false)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(chatId, messageId)
        }
    }

    fun clearChat() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.clearChat(chatId, userId)
        }
    }

    fun deleteChat() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.deleteChat(chatId, userId)
        }
    }

    fun blockUser() {
        val otherUserId = _uiState.value.otherUserId
        if (otherUserId.isBlank()) return
        viewModelScope.launch {
            authRepository.blockUser(otherUserId)
            _uiState.value = _uiState.value.copy(isBlocked = true)
        }
    }

    fun unblockUser() {
        val otherUserId = _uiState.value.otherUserId
        if (otherUserId.isBlank()) return
        viewModelScope.launch {
            authRepository.unblockUser(otherUserId)
            _uiState.value = _uiState.value.copy(isBlocked = false)
        }
    }

    fun reportUser(reason: String) {
        val otherUserId = _uiState.value.otherUserId
        if (otherUserId.isBlank()) return
        viewModelScope.launch {
            authRepository.reportUser(otherUserId, reason)
        }
    }
}
