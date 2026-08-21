package com.example.staybuddy.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.manager.PreferenceManager
import com.example.staybuddy.data.repository.LocationMappingRepository
import com.example.staybuddy.domain.model.AnchorType
import com.example.staybuddy.domain.model.TargetAnchor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val locationMappingRepository: LocationMappingRepository
) : ViewModel() {

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity.asStateFlow()

    val supportedCities: List<String>
        get() = locationMappingRepository.getSupportedCityNames()

    fun selectCity(city: String) {
        _selectedCity.value = if (_selectedCity.value == city) null else city
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            // Persist city selection if made
            _selectedCity.value?.let { city ->
                preferenceManager.setSelectedCity(city)
                val coords = locationMappingRepository.getCityCoordinates(city)
                if (coords != null) {
                    preferenceManager.setTargetAnchor(
                        TargetAnchor(
                            type = AnchorType.CITY,
                            name = city,
                            lat = coords.first,
                            lon = coords.second
                        )
                    )
                }
            }
            preferenceManager.setOnboardingCompleted(true)
        }
    }
}
