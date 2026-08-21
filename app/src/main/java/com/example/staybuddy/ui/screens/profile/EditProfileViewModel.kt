package com.example.staybuddy.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.User
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.repository.AvatarItem
import com.example.staybuddy.data.repository.AvatarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val user: User? = null,
    val name: String = "",
    val phone: String = "",
    val gender: String = "",
    val city: String = "",
    val college: String = "",
    val bio: String = "",
    val existingProfileImageUrl: String = "",
    val newProfileImageUri: Uri? = null,
    val selectedAvatarKey: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val avatarRepository: AvatarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    val activeAvatars: StateFlow<List<AvatarItem>> = avatarRepository.activeAvatars
    val isAvatarFeatureEnabled: StateFlow<Boolean> = avatarRepository.isFeatureEnabled

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
            return
        }

        viewModelScope.launch {
            val result = authRepository.getUserFromFirestore(userId)
            result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        val img = user.profileImage
                        val key = if (img.startsWith("avatar:")) img.removePrefix("avatar:") else null

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = user,
                            name = user.name,
                            phone = user.phone,
                            gender = user.gender,
                            city = user.city,
                            college = user.college,
                            bio = user.bio,
                            existingProfileImageUrl = img,
                            selectedAvatarKey = key
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "User not found"
                        )
                    }
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Failed to fetch user"
                    )
                }
            )
        }
    }

    fun onNameChange(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onPhoneChange(phone: String) { _uiState.value = _uiState.value.copy(phone = phone) }
    fun onGenderChange(gender: String) { _uiState.value = _uiState.value.copy(gender = gender) }
    fun onCityChange(city: String) { _uiState.value = _uiState.value.copy(city = city) }
    fun onCollegeChange(college: String) { _uiState.value = _uiState.value.copy(college = college) }
    fun onBioChange(bio: String) {
        if (bio.length <= 50) {
            _uiState.value = _uiState.value.copy(bio = bio)
        }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            newProfileImageUri = uri,
            selectedAvatarKey = null
        )
    }

    fun onAvatarSelected(avatarKey: String) {
        _uiState.value = _uiState.value.copy(
            existingProfileImageUrl = "avatar:$avatarKey",
            selectedAvatarKey = avatarKey,
            newProfileImageUri = null
        )
    }

    fun saveProfile() {
        val state = _uiState.value
        val currentUser = state.user ?: return

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            var finalImageUrl = state.existingProfileImageUrl

            // Upload new gallery image if selected
            if (state.newProfileImageUri != null) {
                val uploadResult = authRepository.uploadProfileImage(state.newProfileImageUri)
                uploadResult.onSuccess { url ->
                    finalImageUrl = url
                }.onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Failed to upload image: ${it.message}"
                    )
                    return@launch
                }
            }

            val updatedUser = currentUser.copy(
                name = state.name,
                phone = state.phone,
                gender = state.gender,
                city = state.city,
                college = state.college,
                bio = state.bio,
                profileImage = finalImageUrl
            )

            val updateResult = authRepository.updateUser(updatedUser)
            updateResult.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isSaving = false, isSuccess = true)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = it.message ?: "Failed to update profile"
                    )
                }
            )
        }
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val result = authRepository.deleteAccount()
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false)
                onComplete(true, null)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                onComplete(false, e.message ?: "Failed to delete account. Please try logging in again before deleting.")
            }
        }
    }

    fun resolveDisplayUrl(rawUrl: String?): String? {
        return avatarRepository.resolveAvatarUrl(rawUrl)
    }
}
