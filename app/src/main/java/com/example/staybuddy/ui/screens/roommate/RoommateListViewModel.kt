package com.example.staybuddy.ui.screens.roommate

import com.example.staybuddy.data.model.User
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.ChatRepository
import com.example.staybuddy.data.repository.RoommateRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.staybuddy.data.model.RoommatePostType
import com.example.staybuddy.data.model.RoommatePost

import com.example.staybuddy.data.manager.PreferenceManager
import com.example.staybuddy.domain.model.TargetAnchor

import com.example.staybuddy.data.manager.RemoteConfigManager

data class RoommateListUiState(
    val posts: List<RoommatePost> = emptyList(),
    val filteredPosts: List<RoommatePost> = emptyList(),
    val matchScores: Map<String, Int> = emptyMap(), // Maps userId to match percentage
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val maxBudget: Float = 50000f,
    val genderPreference: String = "Any",
    val selectedPostType: RoommatePostType? = RoommatePostType.OFFER, // OFFER (Has Room) vs SEEK (Needs Room) vs null (All)
    val isSeekerModeEnabled: Boolean = false,
    val targetAnchor: TargetAnchor? = null,
    val sortByMatch: Boolean = true,
    val currentUserId: String? = null,
    val currentUserProfile: User? = null,
    val navigateToChatId: String? = null
)

@HiltViewModel
class RoommateListViewModel @Inject constructor(
    private val roommateRepository: RoommateRepository,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val preferenceManager: PreferenceManager,
    private val remoteConfigManager: RemoteConfigManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoommateListUiState())
    val uiState: StateFlow<RoommateListUiState> = _uiState.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        val isSeekerEnabled = remoteConfigManager.isRoommateSeekerEnabled
        _uiState.value = _uiState.value.copy(
            currentUserId = uid,
            isSeekerModeEnabled = isSeekerEnabled,
            selectedPostType = if (isSeekerEnabled) _uiState.value.selectedPostType else RoommatePostType.OFFER
        )
        fetchCurrentUserProfile(uid)
        observeTargetAnchor()
        loadPosts()
    }

    private fun observeTargetAnchor() {
        viewModelScope.launch {
            preferenceManager.targetAnchor.collect { anchor ->
                _uiState.value = _uiState.value.copy(targetAnchor = anchor)
                applyFilters()
            }
        }
    }

    private fun fetchCurrentUserProfile(uid: String?) {
        if (uid == null) return
        viewModelScope.launch {
            authRepository.getUserFromFirestore(uid).onSuccess { user ->
                _uiState.value = _uiState.value.copy(currentUserProfile = user)
                calculateMatchScores()
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            roommateRepository.getRoommatePosts()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load posts"
                    )
                }
                .collect { posts ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        posts = posts,
                        error = null
                    )
                    calculateMatchScores()
                    applyFilters()
                }
        }
    }

    private fun calculateMatchScores() {
        val currentUser = _uiState.value.currentUserProfile ?: return
        val currentUserAnswers = currentUser.quizResults
        if (currentUserAnswers.isEmpty()) return

        val posts = _uiState.value.posts
        val scores = mutableMapOf<String, Int>()

        viewModelScope.launch {
            posts.forEach { post ->
                if (post.userId != currentUser.userId && !scores.containsKey(post.userId)) {
                    authRepository.getUserFromFirestore(post.userId).onSuccess { creator ->
                        creator?.let {
                            val score = calculateCompatibility(currentUser, it, post)
                            scores[post.userId] = score
                            _uiState.value = _uiState.value.copy(matchScores = scores.toMap())
                        }
                    }
                }
            }
        }
    }

    private fun calculateCompatibility(currentUser: User, creatorUser: User, post: RoommatePost): Int {
        val user1Quiz = currentUser.quizResults
        val user2Quiz = creatorUser.quizResults
        
        // Post Explicit Hard Dealbreakers
        if (post.genderPreference.isNotBlank() && post.genderPreference != "Any") {
            if (currentUser.gender != post.genderPreference) return 0
        }
        
        if (post.smokingDrinking == "Non-Smoker" && user1Quiz["smokingDrinking"] == 4) return 0
        if (post.foodPreference == "Vegetarian" && user1Quiz["foodPreference"] == 3) return 0
        if (post.petFriendly == "No Pets Please" && user1Quiz["petFriendly"] == 1) return 0
        
        if (user1Quiz.isEmpty() || user2Quiz.isEmpty()) return 0
        
        var totalScore = 0f
        var maxPossibleScore = 0f
        
        user1Quiz.forEach { (questionId, user1Answer) ->
            user2Quiz[questionId]?.let { user2Answer ->
                // Hard Dealbreakers
                if (questionId == "smokingDrinking") {
                    if ((user1Answer == 1 && user2Answer == 4) || (user1Answer == 4 && user2Answer == 1)) {
                        return 0
                    }
                }
                if (questionId == "petFriendly") {
                    if ((user1Answer >= 3 && user2Answer == 1) || (user1Answer == 1 && user2Answer >= 3)) {
                        return 0 
                    }
                }

                // Question weights
                val weight = when(questionId) {
                    "cleanlinessLevel", "sleepSchedule" -> 1.5f 
                    "smokingDrinking", "petFriendly", "guestsVisitors" -> 1.5f 
                    else -> 1.0f
                }
                
                val diff = kotlin.math.abs(user1Answer - user2Answer)
                val scoreForQuestion = when(diff) {
                    0 -> 100f
                    1 -> 70f
                    2 -> 30f
                    else -> 0f
                }
                
                totalScore += scoreForQuestion * weight
                maxPossibleScore += 100f * weight
            }
        }
        
        return if (maxPossibleScore > 0) ((totalScore / maxPossibleScore) * 100).toInt() else 0
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onMaxBudgetChanged(maxBudget: Float) {
        _uiState.value = _uiState.value.copy(maxBudget = maxBudget)
        applyFilters()
    }

    fun onGenderPreferenceChanged(gender: String) {
        _uiState.value = _uiState.value.copy(genderPreference = gender)
        applyFilters()
    }

    fun onPostTypeSelected(type: RoommatePostType?) {
        _uiState.value = _uiState.value.copy(selectedPostType = type)
        applyFilters()
    }

    fun onSortByMatchChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(sortByMatch = enabled)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.posts.filter { post ->
            val matchesType = when (state.selectedPostType) {
                RoommatePostType.OFFER -> post.postType == RoommatePostType.OFFER
                RoommatePostType.SEEK -> post.postType == RoommatePostType.SEEK
                null -> true
            }
            
            val matchesSearch = if (state.searchQuery.isNotBlank()) {
                post.location.contains(state.searchQuery, ignoreCase = true) ||
                post.city.contains(state.searchQuery, ignoreCase = true) ||
                post.description.contains(state.searchQuery, ignoreCase = true) ||
                post.userName.contains(state.searchQuery, ignoreCase = true)
            } else true

            val matchesBudget = post.priceShare <= state.maxBudget

            val matchesGender = if (state.genderPreference != "Any") {
                val postGender = post.genderPreference.takeIf { it.isNotBlank() } ?: "Any"
                postGender.equals(state.genderPreference, ignoreCase = true) || postGender.equals("Any", ignoreCase = true)
            } else true

            matchesType && matchesSearch && matchesBudget && matchesGender
        }

        val sorted = if (state.sortByMatch) {
            filtered.sortedByDescending { state.matchScores[it.userId] ?: 0 }
        } else filtered

        _uiState.value = state.copy(filteredPosts = sorted)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun createChat(roommateUserId: String, roommatePostId: String? = null) {
        val currentUserId = _uiState.value.currentUserId ?: return
        if (currentUserId == roommateUserId) {
            _uiState.value = _uiState.value.copy(error = "You cannot chat with yourself.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = chatRepository.getOrCreateChatRoom(
                user1Id = currentUserId, 
                user2Id = roommateUserId, 
                roommatePostId = roommatePostId
            )
            result.onSuccess { chatId ->
                _uiState.value = _uiState.value.copy(isLoading = false, navigateToChatId = chatId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to start chat")
            }
        }
    }

    fun onChatNavigated() {
        _uiState.value = _uiState.value.copy(navigateToChatId = null)
    }
}
