package com.example.staybuddy.ui.screens.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.data.repository.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.staybuddy.data.model.Inquiry
import com.example.staybuddy.data.repository.FavoriteRepository
import com.example.staybuddy.data.repository.InquiryRepository
import com.example.staybuddy.data.repository.ReportRepository
import com.example.staybuddy.data.repository.ReviewRepository
import com.example.staybuddy.data.repository.ChatRepository
import com.example.staybuddy.data.model.Review
import com.google.firebase.auth.FirebaseAuth

data class ListingDetailUiState(
    val listing: PgListing? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false,
    val isOwnerView: Boolean = false,
    val isInquirySent: Boolean = false,
    val hasExistingInquiry: Boolean = false,
    val isReportSent: Boolean = false,
    val reportError: String? = null,
    val hasAlreadyReported: Boolean = false,
    val navigateToChatId: String? = null,
    val showInquiryDialog: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val userReview: Review? = null,
    val isReviewSubmitting: Boolean = false,
    val reviewError: String? = null,
    val similarListings: List<PgListing> = emptyList()
)

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val favoriteRepository: FavoriteRepository,
    private val inquiryRepository: InquiryRepository,
    private val reportRepository: ReportRepository,
    private val chatRepository: ChatRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: com.example.staybuddy.data.repository.UserRepository,
    private val auth: FirebaseAuth,
    private val authRepository: com.example.staybuddy.data.repository.AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {



    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    private val _uiState = MutableStateFlow(ListingDetailUiState())
    val uiState: StateFlow<ListingDetailUiState> = _uiState.asStateFlow()

    init {
        loadListing()
        observeFavoriteStatus()
        checkReportStatus()
        checkExistingInquiry()
        loadReviews()
        loadSimilarListings()
    }

    private fun loadListing() {
        viewModelScope.launch {
            // Check local Room cache first to allow smooth shared transitions & avoid microsecond shimmer
            val cachedResult = listingRepository.getListingById(listingId)
            val cachedListing = cachedResult.getOrNull()

            if (cachedListing != null) {
                val currentUid = auth.currentUser?.uid
                val isOwner = currentUid != null && cachedListing.ownerId == currentUid
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    listing = cachedListing,
                    isOwnerView = isOwner
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val result = if (cachedListing != null) cachedResult else listingRepository.getListingById(listingId)
            
            result.onSuccess { listing ->
                if (listing != null) {
                    val currentUid = auth.currentUser?.uid
                    val isOwner = currentUid != null && listing.ownerId == currentUid
                    val ownerResult = userRepository.getUser(listing.ownerId)
                    val ownerUser = ownerResult.getOrNull()
                    val isOwnerVerified = ownerUser?.let { 
                        it.isOwnerVerified || it.verificationStatus == "VERIFIED" 
                    } ?: false
                    val fetchedOwnerImage = ownerUser?.profileImage?.takeIf { it.isNotBlank() } ?: listing.ownerProfileImage

                    val finalListing = listing.copy(
                        isVerified = if (isOwnerVerified) true else listing.isVerified,
                        ownerProfileImage = fetchedOwnerImage
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        listing = finalListing,
                        isOwnerView = isOwner
                    )
                    // Increment real view count if tenant is viewing
                    if (!isOwner) {
                        listingRepository.incrementViewCount(listingId)
                    }
                } else if (_uiState.value.listing == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, listing = null)
                }
            }.onFailure { exception ->
                if (_uiState.value.listing == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load listing"
                    )
                }
            }
        }
    }
    
    private fun observeFavoriteStatus() {
        viewModelScope.launch {
            favoriteRepository.getFavoriteListingIds()
                .catch { /* ignore */ }
                .collect { ids ->
                    _uiState.value = _uiState.value.copy(isFavorite = ids.contains(listingId))
                }
        }
    }
    
    fun toggleFavorite() {
        viewModelScope.launch {
            // Optimistically toggle locally to reduce visual lag
            val wasFavorite = _uiState.value.isFavorite
            _uiState.value = _uiState.value.copy(isFavorite = !wasFavorite)

            val result = if (wasFavorite) {
                favoriteRepository.removeFavorite(listingId)
            } else {
                favoriteRepository.addFavorite(listingId)
            }
            
            if (result.isFailure) {
                // Revert if failed
                _uiState.value = _uiState.value.copy(isFavorite = wasFavorite)
            }
        }
    }

    fun sendInquiry(moveInDate: Long, roomType: String, message: String) {
        val currentListing = _uiState.value.listing ?: return
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val currentUserProfile = authRepository.getLocalUser(userId)
            val inquiry = Inquiry(
                listingId = currentListing.listingId,
                userId = userId,
                userName = currentUserProfile?.name ?: auth.currentUser?.displayName ?: "Student",
                userPhotoUrl = currentUserProfile?.profileImage?.takeIf { it.isNotBlank() } ?: auth.currentUser?.photoUrl?.toString() ?: "",
                hostId = currentListing.ownerId,
                moveInDate = moveInDate,
                roomType = roomType,
                message = message
            )
            val result = inquiryRepository.sendInquiry(inquiry)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isInquirySent = true, 
                    hasExistingInquiry = true,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = e.message ?: "Failed to send inquiry"
                )
            }
        }
    }

    private fun checkExistingInquiry() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val existing = inquiryRepository.getExistingInquiry(userId, listingId)
            _uiState.value = _uiState.value.copy(hasExistingInquiry = existing != null)
        }
    }

    private fun checkReportStatus() {
        viewModelScope.launch {
            try {
                val reported = reportRepository.hasUserReported(listingId)
                _uiState.value = _uiState.value.copy(hasAlreadyReported = reported)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(hasAlreadyReported = false)
            }
        }
    }

    fun reportListing(reason: String) {
        viewModelScope.launch {
            val result = reportRepository.reportListing(listingId, reason)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isReportSent = true,
                    hasAlreadyReported = true,
                    reportError = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    reportError = e.message ?: "Failed to submit report"
                )
            }
        }
    }

    fun createChat() {
        val currentListing = _uiState.value.listing ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        if (currentUserId == currentListing.ownerId) {
            _uiState.value = _uiState.value.copy(error = "You cannot chat with yourself.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = chatRepository.getOrCreateChatRoom(
                user1Id = currentUserId, 
                user2Id = currentListing.ownerId, 
                listingId = currentListing.listingId
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setInquiryDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showInquiryDialog = visible)
    }

    private fun loadSimilarListings() {
        val current = _uiState.value.listing ?: return
        viewModelScope.launch {
            listingRepository.getListingsByCity(current.city)
                .catch { /* ignore errors for similar listings */ }
                .collect { cityListings ->
                    val similar = cityListings
                        .filter { it.listingId != current.listingId && it.isActive }
                        .filter { it.roomType.equals(current.roomType, ignoreCase = true) }
                        .sortedBy { kotlin.math.abs(it.price - current.price) }
                        .take(6)
                    _uiState.value = _uiState.value.copy(similarListings = similar)
                }
        }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            val reviewsResult = reviewRepository.getReviewsForListing(listingId)
            val userReviewResult = reviewRepository.getUserReviewForListing(listingId)

            _uiState.value = _uiState.value.copy(
                reviews = reviewsResult.getOrDefault(emptyList()),
                userReview = userReviewResult.getOrNull()
            )
        }
    }

    fun submitReview(rating: Int, comment: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReviewSubmitting = true, reviewError = null)
            val result = reviewRepository.submitReview(listingId, rating, comment)
            
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isReviewSubmitting = false)
                loadReviews() // Reload reviews to show the new one
                loadListing() // Reload listing to update the average rating
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isReviewSubmitting = false,
                    reviewError = e.message ?: "Failed to submit review"
                )
            }
        }
    }
}
