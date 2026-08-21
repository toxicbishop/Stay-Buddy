package com.example.staybuddy.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.repository.LocationService
import com.example.staybuddy.data.repository.LocationSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.staybuddy.data.repository.LocationRepository

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val locationService: LocationService,
    private val locationRepository: LocationRepository
) : ViewModel() {

    suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return locationRepository.getCurrentLocation()
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _suggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val suggestions: StateFlow<List<LocationSuggestion>> = _suggestions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    fun setCurrentLocation(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            delay(50) // Ultra-fast 50ms debounce for instant live keystroke results
            val results = locationService.autocomplete(
                query = query,
                biasLat = currentLat,
                biasLon = currentLon
            )
            _suggestions.value = results
            _isLoading.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _suggestions.value = emptyList()
    }
}
