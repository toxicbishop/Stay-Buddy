package com.example.staybuddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.ChatRepository
import com.example.staybuddy.data.repository.InquiryRepository
import com.example.staybuddy.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.staybuddy.utils.AppUpdater
import com.example.staybuddy.utils.UpdateInfo

import com.example.staybuddy.data.manager.PreferenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class MainUiState(
    val userRole: String = Constants.ROLE_STUDENT,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true,
    val unreadChatCount: Int = 0,
    val unreadInquiryCount: Int = 0,
    val isBanned: Boolean = false,
    val banReason: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val inquiryRepository: InquiryRepository,
    preferenceManager: PreferenceManager,
    @ApplicationContext context: Context
) : ViewModel() {

    val appUpdater = AppUpdater(context)
    val updateState: StateFlow<UpdateInfo> = appUpdater.updateState

    val themePreference: StateFlow<String> = preferenceManager.themePreference.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "SYSTEM"
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Guard Jobs — prevent duplicate listener launches on tab switches
    private var unreadChatJob: kotlinx.coroutines.Job? = null
    private var unreadInquiryJob: kotlinx.coroutines.Job? = null
    private var fcmSynced = false

    init {
        refreshSession()
        appUpdater.checkForUpdates()
    }

    fun refreshSession() {
        val currentUser = authRepository.currentUser
        if (currentUser == null) {
            _uiState.value = MainUiState(isLoading = false, isLoggedIn = false)
            return
        }

        viewModelScope.launch {
            val result = authRepository.getUserFromFirestore(currentUser.uid)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    userRole = user?.role ?: Constants.ROLE_STUDENT,
                    isLoggedIn = true,
                    isLoading = false,
                    isBanned = user?.isActive == false,
                    banReason = user?.banReason
                )

                // Real-time ban listener — immediately kicks user if banned mid-session
                startBanListener(currentUser.uid)

                // Sync FCM Token once per session
                if (!fcmSynced) {
                    fcmSynced = true
                    viewModelScope.launch {
                        authRepository.getAndSyncFcmToken()
                    }
                }
                
                // Collect unread chat count — launch ONCE, not on every tab switch
                if (unreadChatJob == null) {
                    unreadChatJob = viewModelScope.launch {
                        chatRepository.getUnreadChatsCount(currentUser.uid).collect { count ->
                            _uiState.value = _uiState.value.copy(unreadChatCount = count)
                        }
                    }
                }
                
                // Collect unread inquiries count — launch ONCE for Owners
                if (user?.role == Constants.ROLE_OWNER && unreadInquiryJob == null) {
                    unreadInquiryJob = viewModelScope.launch {
                        inquiryRepository.getPendingInquiriesCount(currentUser.uid).collect { count ->
                            _uiState.value = _uiState.value.copy(unreadInquiryCount = count)
                        }
                    }
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    userRole = Constants.ROLE_STUDENT,
                    isLoggedIn = true,
                    isLoading = false
                )
            }
        }
    }

    private var banListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    private fun startBanListener(userId: String) {
        // Remove previous listener if exists
        banListenerRegistration?.remove()

        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        banListenerRegistration = firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val isActive = snapshot.getBoolean("isActive") ?: true
                if (!isActive) {
                    val reason = snapshot.getString("banReason")
                    android.util.Log.d("MainViewModel", "User banned in real-time: $reason")
                    _uiState.value = _uiState.value.copy(isBanned = true, banReason = reason)
                    // Sign out will be handled by the UI observing isBanned
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        banListenerRegistration?.remove()
        appUpdater.cleanup()
    }
}
