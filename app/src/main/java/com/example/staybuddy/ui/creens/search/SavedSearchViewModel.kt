package com.example.staybuddy.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.SavedSearch
import com.example.staybuddy.data.repository.SavedSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedSearchUiState(
    val savedSearches: List<SavedSearch> = emptyList(),
    val isLoading: Boolean = false,
    val showSaveDialog: Boolean = false,
    val showListSheet: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SavedSearchViewModel @Inject constructor(
    private val savedSearchRepository: SavedSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedSearchUiState())
    val uiState: StateFlow<SavedSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            savedSearchRepository.getSavedSearches().collect { searches ->
                _uiState.update { it.copy(savedSearches = searches, isLoading = false) }
            }
        }
    }

    fun showSaveDialog(show: Boolean) {
        _uiState.update { it.copy(showSaveDialog = show) }
    }

    fun showListSheet(show: Boolean) {
        _uiState.update { it.copy(showListSheet = show) }
    }

    fun saveSearch(search: SavedSearch) {
        viewModelScope.launch {
            val result = savedSearchRepository.saveSearch(search)
            if (result.isFailure) {
                _uiState.update { it.copy(error = "Failed to save search") }
            } else {
                _uiState.update { it.copy(showSaveDialog = false) }
            }
        }
    }

    fun deleteSearch(searchId: String) {
        viewModelScope.launch {
            savedSearchRepository.deleteSearch(searchId)
        }
    }

    fun toggleNotification(searchId: String, enabled: Boolean) {
        viewModelScope.launch {
            savedSearchRepository.toggleNotification(searchId, enabled)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
