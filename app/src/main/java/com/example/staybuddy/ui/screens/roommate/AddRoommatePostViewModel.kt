package com.example.staybuddy.ui.screens.roommate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.RoommatePost
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.RoommateRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.staybuddy.data.manager.RemoteConfigManager
import com.example.staybuddy.data.model.RoommatePostType

data class AddRoommatePostUiState(
    val city: String = "Vadodara",
    val location: String = "",
    val description: String = "",
    val priceShare: String = "",
    val availableBeds: String = "",
    val totalBeds: String = "",
    val roomType: String = "Shared",
    val postType: RoommatePostType = RoommatePostType.OFFER,
    val isSeekerModeEnabled: Boolean = false,
    val preferences: Map<String, String> = emptyMap(),
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Core Preferences
    val guestsVisitors: String = "Any",
    val genderPreference: String = "Any",
    val sleepSchedule: String = "Any",
    val cleanlinessLevel: String = "Any",
    val foodPreference: String = "Any",
    val smokingDrinking: String = "Any",
    val petFriendly: String = "Any",
    
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddRoommatePostViewModel @Inject constructor(
    private val roommateRepository: RoommateRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
    private val locationService: com.example.staybuddy.data.repository.LocationService,
    private val remoteConfigManager: RemoteConfigManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRoommatePostUiState(
        isSeekerModeEnabled = remoteConfigManager.isRoommateSeekerEnabled
    ))
    val uiState: StateFlow<AddRoommatePostUiState> = _uiState.asStateFlow()

    private val postId: String? = savedStateHandle.get<String>("postId")?.takeIf { it != "new" }

    init {
        if (postId != null) {
            _uiState.value = _uiState.value.copy(isEditing = true, isLoading = true)
            viewModelScope.launch {
                roommateRepository.getRoommatePostById(postId).onSuccess { post ->
                    if (post != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            city = post.city,
                            location = post.location,
                            description = post.description,
                            priceShare = post.priceShare.toString(),
                            availableBeds = post.availableBeds.toString(),
                            totalBeds = post.totalBeds.toString(),
                            roomType = post.roomType,
                            postType = post.postType,
                            preferences = post.preferences,
                            address = post.address,
                            latitude = post.latitude,
                            longitude = post.longitude,
                            guestsVisitors = post.guestsVisitors,
                            genderPreference = post.genderPreference.takeIf { it.isNotBlank() } ?: "Any",
                            sleepSchedule = post.sleepSchedule.takeIf { it.isNotBlank() } ?: "Any",
                            cleanlinessLevel = post.cleanlinessLevel.takeIf { it.isNotBlank() } ?: "Any",
                            foodPreference = post.foodPreference.takeIf { it.isNotBlank() } ?: "Any",
                            smokingDrinking = post.smokingDrinking.takeIf { it.isNotBlank() } ?: "Any",
                            petFriendly = post.petFriendly.takeIf { it.isNotBlank() } ?: "Any"
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = "Post not found")
                    }
                }.onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to fetch post")
                }
            }
        }
    }

    fun updateField(field: String, value: String) {
        _uiState.value = when (field) {
            "city" -> _uiState.value.copy(city = value)
            "location" -> _uiState.value.copy(location = value)
            "description" -> _uiState.value.copy(description = value)
            "priceShare" -> _uiState.value.copy(priceShare = value)
            "availableBeds" -> _uiState.value.copy(availableBeds = value)
            "totalBeds" -> _uiState.value.copy(totalBeds = value)
            "roomType" -> _uiState.value.copy(roomType = value)
            "address" -> _uiState.value.copy(address = value)
            "guestsVisitors" -> _uiState.value.copy(guestsVisitors = value)
            "genderPreference" -> _uiState.value.copy(genderPreference = value)
            "sleepSchedule" -> _uiState.value.copy(sleepSchedule = value)
            "cleanlinessLevel" -> _uiState.value.copy(cleanlinessLevel = value)
            "foodPreference" -> _uiState.value.copy(foodPreference = value)
            "smokingDrinking" -> _uiState.value.copy(smokingDrinking = value)
            "petFriendly" -> _uiState.value.copy(petFriendly = value)
            else -> _uiState.value
        }
    }

    fun onPostTypeChanged(type: RoommatePostType) {
        _uiState.value = _uiState.value.copy(postType = type)
    }

    fun setPostType(type: RoommatePostType) {
        _uiState.value = _uiState.value.copy(postType = type)
    }
    
    fun updatePreference(key: String, value: String) {
        val currentPrefs = _uiState.value.preferences.toMutableMap()
        if (value.isBlank()) {
            currentPrefs.remove(key)
        } else {
            currentPrefs[key] = value
        }
        _uiState.value = _uiState.value.copy(preferences = currentPrefs)
    }

    fun submitPost() {
        val state = _uiState.value
        val userId = auth.currentUser?.uid
        
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "User not authenticated")
            return
        }
        
        if (state.city.isBlank() || state.location.isBlank() || state.priceShare.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all required fields")
            return
        }

        val price = state.priceShare.toIntOrNull()
        val availableBeds = state.availableBeds.toIntOrNull() ?: 0
        val totalBeds = state.totalBeds.toIntOrNull() ?: 0
        
        if (price == null) {
            _uiState.value = _uiState.value.copy(error = "Price must be a valid number")
            return
        }

        if (totalBeds > 0 && availableBeds > totalBeds) {
            _uiState.value = _uiState.value.copy(error = "Available beds cannot exceed total beds")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val user = kotlinx.coroutines.withTimeoutOrNull(5000) {
                    authRepository.getUserFromFirestore(userId).getOrNull()
                }
                
                val post = RoommatePost(
                    postId = postId ?: "",
                    userId = userId,
                    city = state.city,
                    location = state.location,
                    description = state.description,
                    priceShare = price,
                    availableBeds = availableBeds,
                    totalBeds = totalBeds,
                    roomType = state.roomType,
                    postType = state.postType,
                    preferences = state.preferences,
                    address = state.address,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    guestsVisitors = if (state.guestsVisitors == "Any") "" else state.guestsVisitors,
                    genderPreference = if (state.genderPreference == "Any") "" else state.genderPreference,
                    sleepSchedule = if (state.sleepSchedule == "Any") "" else state.sleepSchedule,
                    cleanlinessLevel = if (state.cleanlinessLevel == "Any") "" else state.cleanlinessLevel,
                    foodPreference = if (state.foodPreference == "Any") "" else state.foodPreference,
                    smokingDrinking = if (state.smokingDrinking == "Any") "" else state.smokingDrinking,
                    petFriendly = if (state.petFriendly == "Any") "" else state.petFriendly,
                    userName = user?.name ?: auth.currentUser?.displayName ?: "User",
                    userProfileImage = user?.profileImage ?: auth.currentUser?.photoUrl?.toString() ?: "",
                    userPhone = user?.phone ?: ""
                )
                
                val result = if (postId != null) {
                    roommateRepository.updateRoommatePost(post)
                } else {
                    roommateRepository.addRoommatePost(post).map { Unit }
                }
                
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to save post"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
    
    fun updateLocation(lat: Double, lon: Double, address: String) {
        _uiState.value = _uiState.value.copy(
            latitude = lat,
            longitude = lon,
            address = address
        )

        // Auto-fill city, locality, and address from GPS coordinates
        viewModelScope.launch {
            try {
                val city = locationService.detectCityFromGps(lat, lon)
                val area = locationService.detectAreaFromGps(lat, lon, city ?: "")
                val addressResult = locationService.reverseGeocode(lat, lon)?.displayName
                
                _uiState.value = _uiState.value.copy(
                    city = city ?: _uiState.value.city,
                    location = area ?: _uiState.value.location,
                    address = addressResult ?: _uiState.value.address
                )
            } catch (e: Exception) {
                // Silently fail - user can manually enter
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
