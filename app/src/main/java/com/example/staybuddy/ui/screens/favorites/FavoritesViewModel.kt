package com.example.staybuddy.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.data.repository.FavoriteRepository
import com.example.staybuddy.data.repository.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favoriteListings: List<PgListing> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private var displayedIds: Set<String>? = null

    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                // Combine all listings and favorite IDs
                combine(
                    listingRepository.getListings().catch { emit(emptyList()) },
                    favoriteRepository.getFavoriteListingIds().catch { emit(emptyList()) }
                ) { allListings, favoriteIds ->
                    if (displayedIds == null) {
                        displayedIds = favoriteIds.toSet()
                    } else {
                        // Keep previously displayed items, but add any newly favorited items
                        displayedIds = displayedIds!! + favoriteIds.toSet()
                    }

                    val favorites = allListings.filter { it.listingId in displayedIds!! }
                    _uiState.value.copy(
                        favoriteListings = favorites,
                        favoriteIds = favoriteIds.toSet(),
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load favorites"
                    )
                }
            }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Add a small delay for better visual feedback during pull-to-refresh
            kotlinx.coroutines.delay(500)
            
            // Sync displayed items exactly with the current true favorites in DB
            displayedIds = _uiState.value.favoriteIds.toSet()
            
            _uiState.update { state ->
                state.copy(
                    favoriteListings = state.favoriteListings.filter { it.listingId in displayedIds!! },
                    isRefreshing = false
                )
            }
        }
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            if (_uiState.value.favoriteIds.contains(listingId)) {
                favoriteRepository.removeFavorite(listingId)
            } else {
                favoriteRepository.addFavorite(listingId)
            }
        }
    }
}
