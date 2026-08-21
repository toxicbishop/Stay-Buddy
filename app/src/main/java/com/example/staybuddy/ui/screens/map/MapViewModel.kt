package com.example.staybuddy.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.manager.PreferenceManager
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.data.repository.ListingRepository
import com.example.staybuddy.domain.model.AnchorType
import com.example.staybuddy.domain.model.TargetAnchor
import com.example.staybuddy.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import com.mapbox.geojson.Point
import javax.inject.Inject

data class MapUiState(
    val listings: List<PgListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedListing: PgListing? = null,
    val userLocation: Point? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            // Combine listings with the user's saved city + anchor so the map
            // always reflects the same location context as the Home screen.
            combine(
                listingRepository.getListings().onStart { _uiState.value = _uiState.value.copy(isLoading = true, error = null) },
                preferenceManager.selectedCity,
                preferenceManager.targetAnchor
            ) { listings, city, anchor ->
                Triple(listings, city ?: "Vadodara", anchor)
            }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    )
                }
                .collect { (listings, city, anchor) ->
                    val locationToUse: Pair<Double, Double>? = anchor
                        ?.takeIf { it.lat != 0.0 && it.lon != 0.0 }
                        ?.let { Pair(it.lat, it.lon) }

                    // Compute distance from user's anchor for each listing
                    val listingsWithDist = listings.map { listing ->
                        val dist = if (
                            listing.latitude != 0.0 && listing.longitude != 0.0 && locationToUse != null
                        ) {
                            LocationUtils.calculateDistance(
                                listing.latitude, listing.longitude,
                                locationToUse.first, locationToUse.second
                            )
                        } else null
                        listing to dist
                    }

                    val now = System.currentTimeMillis()

                    // Sort: Premium → Boosted/Featured in same city → Same city first → Distance asc
                    val sorted = listingsWithDist.sortedWith(
                        compareBy<Pair<PgListing, Double?>> { !it.first.isPremium }
                            .thenBy { (l, _) ->
                                val cityMatch = l.city.contains(city, ignoreCase = true)
                                val boostTime = l.boostExpiresAt?.toDate()?.time
                                val featuredTime = l.featuredUntil?.toDate()?.time
                                val isBoostedInCity = boostTime != null && boostTime > now && cityMatch
                                val isFeaturedInCity = featuredTime != null && featuredTime > now && cityMatch
                                !(isBoostedInCity || isFeaturedInCity)
                            }
                            .thenBy { (l, _) -> if (l.city.contains(city, ignoreCase = true)) 0 else 1 }
                            .thenBy { it.second ?: Double.MAX_VALUE }
                    )

                    // Show: selected city + within 15 km radius (same threshold as Home "nearby")
                    val filtered = sorted.filter { (l, dist) ->
                        val inCity = l.city.contains(city, ignoreCase = true)
                        val isNear = dist != null && dist < 15.0
                        inCity || isNear
                    }.map { it.first }

                    // Derive userLocation from anchor (for map centering + blue-dot)
                    val mapCenter = locationToUse?.let { Point.fromLngLat(it.second, it.first) }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        listings = filtered,
                        userLocation = mapCenter
                    )
                }
        }
    }

    fun selectListing(listing: PgListing?) {
        _uiState.value = _uiState.value.copy(selectedListing = listing)
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(userLocation = Point.fromLngLat(lng, lat))
    }
}
