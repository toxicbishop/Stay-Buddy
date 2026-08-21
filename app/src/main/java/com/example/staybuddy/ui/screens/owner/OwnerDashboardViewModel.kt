package com.example.staybuddy.ui.screens.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.data.repository.ImageStorageRepository
import com.example.staybuddy.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.example.staybuddy.data.model.Inquiry
import com.example.staybuddy.data.repository.ChatRepository
import com.example.staybuddy.data.repository.InquiryRepository
import javax.inject.Inject

import com.example.staybuddy.data.model.User
import com.example.staybuddy.data.repository.UserRepository
import android.net.Uri

data class OwnerDashboardUiState(
    val listings: List<PgListing> = emptyList(),
    val inquiries: List<Inquiry> = emptyList(),
    val currentUser: User? = null,
    val isUploadingDoc: Boolean = false,
    val isBannerDismissed: Boolean = false,
    val totalListings: Int = 0,
    val activeListings: Int = 0,
    val totalViews: Int = 0,
    val estimatedMonthlyRevenue: Int = 0,
    val messagesCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,
    val navigateToChatId: String? = null
)

@HiltViewModel
class OwnerDashboardViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val imageStorageRepository: ImageStorageRepository,
    private val inquiryRepository: InquiryRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val preferenceManager: com.example.staybuddy.data.manager.PreferenceManager,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerDashboardUiState())
    val uiState: StateFlow<OwnerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadOwnerProfile()
        loadOwnerListings()
        loadOwnerInquiries()
        observeBannerDismissedState()
    }

    private fun observeBannerDismissedState() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            preferenceManager.isVerifiedBannerDismissed(userId).collect { dismissed ->
                _uiState.value = _uiState.value.copy(isBannerDismissed = dismissed)
            }
        }
    }

    fun dismissVerifiedBanner() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            preferenceManager.setVerifiedBannerDismissed(userId, true)
            _uiState.value = _uiState.value.copy(isBannerDismissed = true)
        }
    }

    private fun loadOwnerProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.observeUser(userId)
                .catch { }
                .collect { user ->
                    _uiState.value = _uiState.value.copy(currentUser = user)
                }
        }
    }

    fun uploadVerificationDocument(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingDoc = true, error = null)
            val uploadResult = imageStorageRepository.uploadSingleImage(uri, "staybuddy/verification_docs")
            uploadResult.onSuccess { docUrl ->
                val submitResult = userRepository.submitOwnerVerification(userId, docUrl)
                submitResult.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isUploadingDoc = false,
                        successMessage = "Verification document submitted successfully for admin review!"
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingDoc = false,
                        error = e.message ?: "Failed to save verification document"
                    )
                }
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isUploadingDoc = false,
                    error = e.message ?: "Failed to upload document image"
                )
            }
        }
    }

    private fun loadOwnerInquiries() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            inquiryRepository.getInquiriesForHost(userId)
                .catch { e ->
                    // Handle error if needed
                }
                .collect { inquiries ->
                    _uiState.value = _uiState.value.copy(inquiries = inquiries)
                }
        }
    }

    private fun loadOwnerListings() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "User not authenticated"
            )
            return
        }

        viewModelScope.launch {
            listingRepository.getListingsByOwner(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load listings"
                    )
                }
                .collect { listings ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        listings = listings,
                        totalListings = listings.size,
                        activeListings = listings.count { it.isActive },
                        totalViews = listings.sumOf { it.viewCount },
                        estimatedMonthlyRevenue = listings.filter { it.isActive }.sumOf { it.price },
                        error = null
                    )
                }
        }
    }

    fun toggleListingActiveStatus(listing: PgListing) {
        viewModelScope.launch {
            val updatedListing = listing.copy(isActive = !listing.isActive)
            val result = listingRepository.updateListing(updatedListing)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = "Failed to update listing status")
            } else {
                _uiState.value = _uiState.value.copy(successMessage = "Listing status updated")
            }
        }
    }

    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Optional, but good UX
            
            // Delete associated images first
            val storageResult = imageStorageRepository.deleteListingImages(listingId)
            
            // Delete the document regardless to ensure it's removed
            val dbResult = listingRepository.deleteListing(listingId)
            
            if (dbResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete listing"
                )
            } else {
                 _uiState.value = _uiState.value.copy(isLoading = false, error = null, successMessage = "Listing deleted successfully")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun updateInquiryStatus(inquiryId: String, status: String, inquiry: Inquiry? = null) {
        val ownerId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = inquiryRepository.updateInquiryStatus(inquiryId, status)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(error = "Failed to update inquiry status")
                return@launch
            }
            if (status == "ACCEPTED" && inquiry != null) {
                val chatResult = chatRepository.getOrCreateChatRoom(
                    user1Id = ownerId,
                    user2Id = inquiry.userId,
                    listingId = inquiry.listingId
                )
                chatResult.onSuccess { chatId ->
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Inquiry accepted! Chat created.",
                        navigateToChatId = chatId
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Inquiry accepted, but failed to create chat."
                    )
                }
            }
        }
    }

    fun onChatNavigated() {
        _uiState.value = _uiState.value.copy(navigateToChatId = null)
    }

    fun sendQuickReply(inquiry: Inquiry, messageText: String) {
        val ownerId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            if (inquiry.status == "PENDING") {
                inquiryRepository.updateInquiryStatus(inquiry.inquiryId, "ACCEPTED")
            }
            val chatResult = chatRepository.getOrCreateChatRoom(
                user1Id = ownerId,
                user2Id = inquiry.userId,
                listingId = inquiry.listingId
            )
            chatResult.onSuccess { chatId ->
                chatRepository.sendMessage(
                    roomId = chatId,
                    senderId = ownerId,
                    text = messageText
                )
                _uiState.value = _uiState.value.copy(
                    successMessage = "Quick reply sent!",
                    navigateToChatId = chatId
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Failed to send quick reply")
            }
        }
    }
}
