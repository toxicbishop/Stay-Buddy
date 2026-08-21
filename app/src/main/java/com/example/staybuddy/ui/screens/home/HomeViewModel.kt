package com.example.staybuddy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.staybuddy.data.model.PgListing
import com.example.staybuddy.data.manager.PreferenceManager
import com.example.staybuddy.data.repository.ListingRepository
import com.example.staybuddy.util.AnalyticsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*
import com.example.staybuddy.data.repository.FavoriteRepository
import com.example.staybuddy.data.repository.AuthRepository
import com.example.staybuddy.data.model.User
import kotlinx.coroutines.flow.update
import com.example.staybuddy.utils.Constants
import com.example.staybuddy.data.repository.LocationRepository
import com.example.staybuddy.domain.ListingRanker
import com.example.staybuddy.domain.model.AutocompletePrediction
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

data class HomeUiState(
    val recommendedListings: List<PgListing> = emptyList(),
    val nearbyListings: List<PgListing> = emptyList(),
    val listingsWithDistance: List<Pair<PgListing, Double?>> = emptyList(),
    val selectedCity: String = "Vadodara",
    val selectedArea: String? = null,
    val availableAreas: List<String> = emptyList(),
    val selectedUniversity: String? = null,
    val userLocation: Pair<Double, Double>? = null,
    val targetAnchor: com.example.staybuddy.domain.model.TargetAnchor? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val userName: String = "",
    val userRole: String = Constants.ROLE_STUDENT,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val searchResults: List<AutocompletePrediction> = emptyList(),
    val isSearching: Boolean = false,
    val unreadNotificationsCount: Int = 0,
    // Promotions & Insights
    val activePromotion: com.example.staybuddy.data.model.Promotion? = null,
    val cityInsight: com.example.staybuddy.data.model.CityInsight? = null,
    val userArea: String? = null,
    val supportedCities: List<String> = emptyList(),
    val supportedUniversities: List<String> = emptyList(),
    val recentLocations: List<com.example.staybuddy.domain.model.TargetAnchor> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val preferenceManager: PreferenceManager,
    private val favoriteRepository: FavoriteRepository,
    private val authRepository: AuthRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val locationRepository: LocationRepository,
    private val locationMappingRepository: com.example.staybuddy.data.repository.LocationMappingRepository,
    private val locationService: com.example.staybuddy.data.repository.LocationService,
    private val notificationDao: com.example.staybuddy.data.local.NotificationDao,
    private val promotionRepository: com.example.staybuddy.data.repository.PromotionRepository
) : ViewModel() {

    private val _searchQueryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Load location reference data from Firestore (falls back to hardcoded defaults)
        viewModelScope.launch {
            locationMappingRepository.initialize()
            _uiState.update { it.copy(
                supportedCities = locationMappingRepository.getSupportedCityNames(),
                supportedUniversities = locationMappingRepository.getSupportedUniversityNames()
            ) }
        }
        observePreferences()
        observeFavorites()
        fetchUserName()
        observeSearchQuery()
        observeNotifications()
        observeSyncStatus()
        analyticsHelper.logScreenView("home")
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            listingRepository.isSyncing.collect { isSyncing ->
                if (!isSyncing && _uiState.value.isLoading) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            try {
                notificationDao.getUnreadCount().collect { count ->
                    _uiState.update { it.copy(unreadNotificationsCount = count) }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun fetchUserName() {
        viewModelScope.launch {
            authRepository.currentUser?.uid?.let { uid ->
                authRepository.getUserFromFirestore(uid).onSuccess { user ->
                    user?.let {
                        // If user has a city saved in profile and local PreferenceManager hasn't overridden it initially
                        if (it.city.isNotEmpty() && _uiState.value.selectedCity == "Vadodara" && preferenceManager.selectedCity.firstOrNull() == null) {
                            preferenceManager.setSelectedCity(it.city)
                            _uiState.update { state -> state.copy(selectedCity = it.city) }
                        }
                        
                        _uiState.update { state -> state.copy(
                            userName = user.name,
                            userRole = user.role.ifEmpty { Constants.ROLE_STUDENT }
                        ) }
                    }
                }
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                preferenceManager.selectedCity,
                preferenceManager.selectedUniversity,
                preferenceManager.targetAnchor
            ) { city, university, anchor ->
                Triple(city, university, anchor)
            }.collect { (city, university, anchor) ->
                _uiState.update { it.copy(
                    selectedCity = city ?: "Vadodara",
                    selectedUniversity = university,
                    targetAnchor = anchor,
                    userLocation = if (anchor?.type == com.example.staybuddy.domain.model.AnchorType.CURRENT_GPS) Pair(anchor.lat, anchor.lon) else null
                ) }
                // Refresh data if location/city changed significantly
                loadData()
            }
        }
        viewModelScope.launch {
            preferenceManager.recentLocations.collect { recents ->
                _uiState.update { it.copy(recentLocations = recents) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            try {
                favoriteRepository.getFavoriteListingIds()
                    .catch { /* Handle Firestore permission errors gracefully */ }
                    .collect { ids ->
                        _uiState.value = _uiState.value.copy(favoriteIds = ids.toSet())
                    }
            } catch (e: Exception) {
                // Ignore favorites loading error - non-critical feature
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            listingRepository.getListings()
                .onStart { 
                    if (_uiState.value.recommendedListings.isEmpty()) {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                }
                .catch { e -> 
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    ) }
                }
                .collect { listings ->
                    val currentState = _uiState.value
                    
                    val anchor = currentState.targetAnchor
                    val locationToUse = anchor?.let { Pair(it.lat, it.lon) }

                    val listingsWithDist = listings.map { listing ->
                        val distance = if (listing.latitude != 0.0 && listing.longitude != 0.0 && locationToUse != null) {
                            com.example.staybuddy.utils.LocationUtils.calculateDistance(
                                listing.latitude, listing.longitude,
                                locationToUse.first, locationToUse.second
                            )
                        } else null
                        listing to distance
                    }

                    // Detect user's area from GPS
                    val detectedArea = if (locationToUse != null) {
                        locationMappingRepository.detectUserArea(
                            locationToUse.first, locationToUse.second, currentState.selectedCity
                        )
                    } else null

                    val now = System.currentTimeMillis()

                    // Strict city visibility: drop listings whose active feature/boost
                    // is scoped to a different city (area scope never hides).
                    val visibleListings = listingsWithDist.filter { (l, _) ->
                        ListingRanker.isVisibleInCity(l, currentState.selectedCity, now)
                    }

                    // Rank: featured ≫ boosted ≫ premium ≫ quality, area-aware, then freshness
                    val sortedListings = visibleListings.sortedWith(
                        compareByDescending<Pair<PgListing, Double?>> { (l, _) ->
                            ListingRanker.rankScore(l, currentState.selectedCity, detectedArea, now)
                        }.thenByDescending { (l, _) -> l.createdAt }
                    )

                    val isCacheEmpty = listings.isEmpty() && currentState.recommendedListings.isEmpty()
                    val shouldKeepLoading = isCacheEmpty && listingRepository.isSyncing.value

                    // ── Threshold expansion: area → nearby areas → full city ──
                    val MIN_LISTINGS = 5
                    val nearby = if (currentState.selectedArea != null) {
                        // Step 1: Get listings in selected area, sorted by distance
                        val areaListings = sortedListings.filter { (l, dist) ->
                            val categoryMatch = currentState.selectedCategory == "All" || l.roomType.equals(currentState.selectedCategory, ignoreCase = true)
                            val cityMatch = l.city.contains(currentState.selectedCity, ignoreCase = true)
                            val areaMatch = l.area.equals(currentState.selectedArea, ignoreCase = true)
                            categoryMatch && cityMatch && areaMatch
                        }.sortedBy { it.second ?: Double.MAX_VALUE }.map { it.first }

                        if (areaListings.size >= MIN_LISTINGS) {
                            areaListings // Enough listings in selected area
                        } else {
                            // Step 2: Expand to nearby areas (within dynamic radius)
                            // Start with 3km, expand to 7km if still not enough
                            val nearbyAreaListings = sortedListings.filter { (l, dist) ->
                                val categoryMatch = currentState.selectedCategory == "All" || l.roomType.equals(currentState.selectedCategory, ignoreCase = true)
                                val cityMatch = l.city.contains(currentState.selectedCity, ignoreCase = true)
                                val nearEnough = dist != null && dist < 7.0
                                val notInSelectedArea = !l.area.equals(currentState.selectedArea, ignoreCase = true)
                                categoryMatch && cityMatch && nearEnough && notInSelectedArea
                            }.sortedBy { it.second ?: Double.MAX_VALUE }.map { it.first }

                            val combined = (areaListings + nearbyAreaListings).distinctBy { it.listingId }

                            if (combined.size >= MIN_LISTINGS) {
                                combined
                            } else {
                                // Step 3: Expand to full city — sorted by distance
                                val cityListings = sortedListings.filter { (l, dist) ->
                                    val categoryMatch = currentState.selectedCategory == "All" || l.roomType.equals(currentState.selectedCategory, ignoreCase = true)
                                    val cityMatch = l.city.contains(currentState.selectedCity, ignoreCase = true)
                                    categoryMatch && cityMatch
                                }.map { it.first }

                                (combined + cityListings).distinctBy { it.listingId }
                            }
                        }
                    } else {
                        // No area selected — show all nearby city listings
                        sortedListings.filter { (l, dist) ->
                            val categoryMatch = currentState.selectedCategory == "All" || l.roomType.equals(currentState.selectedCategory, ignoreCase = true)
                            val isNear = dist != null && dist < 15.0
                            val cityMatch = l.city.contains(currentState.selectedCity, ignoreCase = true)
                            categoryMatch && (isNear || (dist == null && cityMatch))
                        }.map { it.first }
                    }

                    _uiState.update { it.copy(
                        isLoading = shouldKeepLoading,
                        userArea = detectedArea,
                        recommendedListings = sortedListings.filter { (l, _) ->
                            // Promoted-or-premium first (already visibility-filtered + rank-sorted)
                            val isFeatured = l.featuredUntil != null && l.featuredUntil.toDate().time > now
                            val isBoosted = l.boostExpiresAt != null && l.boostExpiresAt.toDate().time > now
                            l.isPremium || isFeatured || isBoosted
                        }.take(5).ifEmpty { sortedListings.take(3) }.map { it.first },
                        nearbyListings = nearby,
                        listingsWithDistance = sortedListings,
                        userLocation = currentState.userLocation ?: locationToUse
                    ) }

                    // Fetch city promotion & insight (non-blocking, best-effort)
                    launch {
                        try {
                            val city = currentState.selectedCity
                            val promo = promotionRepository.getActivePromotion(city)
                            val insight = promotionRepository.getCityInsight(city)
                            _uiState.update { it.copy(
                                activePromotion = promo,
                                cityInsight = insight
                            ) }
                        } catch (_: Exception) { }
                    }
                }
        }
    }

    fun dismissPromotion() {
        _uiState.update { it.copy(activePromotion = null) }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation()?.let { location ->
                val reverseResult = locationRepository.getReverseGeocode(location.first, location.second)
                val anchorName = reverseResult.getOrNull()?.primaryText ?: "Current Location"

                val detectedCity = locationService.detectCityFromGps(location.first, location.second)
                    ?: reverseResult.getOrNull()?.secondaryText?.split(",")?.firstOrNull()?.trim()
                    ?: locationMappingRepository.getNearestSupportedCity(location.first, location.second)
                    ?: _uiState.value.selectedCity

                if (detectedCity.isNotBlank()) {
                    preferenceManager.setSelectedCity(detectedCity)
                }

                val anchor = com.example.staybuddy.domain.model.TargetAnchor(
                    type = com.example.staybuddy.domain.model.AnchorType.CURRENT_GPS,
                    name = anchorName,
                    lat = location.first,
                    lon = location.second
                )
                preferenceManager.setTargetAnchor(anchor)

                _uiState.update { 
                    it.copy(
                        userLocation = location,
                        selectedCity = if (detectedCity.isNotBlank()) detectedCity else it.selectedCity,
                        selectedArea = null,
                        targetAnchor = anchor
                    )
                }
                
                loadData()
            }
        }
    }

    fun updateCity(city: String) {
        viewModelScope.launch {
            preferenceManager.setSelectedCity(city)
            
            // Set basic anchor for city center to trigger distance calculations
            val latLon = locationMappingRepository.getCityCoordinates(city) ?: Pair(0.0, 0.0)
            if (latLon.first != 0.0) {
                preferenceManager.setTargetAnchor(
                    com.example.staybuddy.domain.model.TargetAnchor(
                        type = com.example.staybuddy.domain.model.AnchorType.CITY,
                        name = city,
                        lat = latLon.first,
                        lon = latLon.second
                    )
                )
            }
            
            // Save to Firestore so it persists across logins
            authRepository.currentUser?.uid?.let { uid ->
                authRepository.updateUserCity(uid, city)
            }

            // Load areas for this city
            val areas = locationMappingRepository.getAreasForCity(city).map { it.name }
            _uiState.update { it.copy(availableAreas = areas, selectedArea = null) }
        }
    }

    fun updateArea(area: String?) {
        _uiState.update { it.copy(selectedArea = area) }
        // Re-filter listings with new area
        loadData()
    }

    fun updateUniversity(uni: String) {
        viewModelScope.launch {
            preferenceManager.setSelectedUniversity(uni)
            
            val latLon = locationMappingRepository.getUniversityCoordinates(uni) ?: Pair(0.0, 0.0)
            if (latLon.first != 0.0) {
                preferenceManager.setTargetAnchor(
                    com.example.staybuddy.domain.model.TargetAnchor(
                        type = com.example.staybuddy.domain.model.AnchorType.UNIVERSITY,
                        name = uni,
                        lat = latLon.first,
                        lon = latLon.second
                    )
                )
            }
        }
    }
    
    fun updateCustomArea(prediction: com.example.staybuddy.domain.model.AutocompletePrediction) {
        viewModelScope.launch {
            // Update selected city from prediction
            val detectedCity = locationService.detectCityFromGps(prediction.lat, prediction.lon)
            val city = detectedCity ?: prediction.secondaryText.split(",").firstOrNull()?.trim() ?: ""
            if (city.isNotBlank()) {
                preferenceManager.setSelectedCity(city)
            }
            preferenceManager.setTargetAnchor(
                com.example.staybuddy.domain.model.TargetAnchor(
                    type = com.example.staybuddy.domain.model.AnchorType.CUSTOM_AREA,
                    name = prediction.primaryText,
                    lat = prediction.lat,
                    lon = prediction.lon
                )
            )
            onSearchQueryChange("")
        }
    }
    
    fun updateTargetAnchor(anchor: com.example.staybuddy.domain.model.TargetAnchor) {
        viewModelScope.launch {
            // Detect city from anchor coordinates so the subtitle (selectedCity) stays in sync
            if (anchor.lat != 0.0 && anchor.lon != 0.0) {
                val detectedCity = locationService.detectCityFromGps(anchor.lat, anchor.lon)
                val city = detectedCity
                    ?: _uiState.value.selectedCity // fallback: keep current if detection fails
                if (city.isNotBlank()) {
                    preferenceManager.setSelectedCity(city)
                }
            }
            preferenceManager.setTargetAnchor(anchor)
            onSearchQueryChange("")
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

    fun updateCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadData()
    }


    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQueryFlow
                .debounce(150)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val clean = query.trim()
                    if (clean.length < 2) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(isSearching = true) }
                    try {
                        val bias = _uiState.value.userLocation
                        val results = locationService.autocomplete(
                            query = clean,
                            biasLat = bias?.first,
                            biasLon = bias?.second
                        )
                        val predictions = results.mapIndexed { index, suggestion ->
                            com.example.staybuddy.domain.model.AutocompletePrediction(
                                placeId = System.currentTimeMillis() + index,
                                primaryText = suggestion.displayName,
                                secondaryText = suggestion.city,
                                lat = suggestion.lat,
                                lon = suggestion.lon
                            )
                        }
                        _uiState.update { it.copy(searchResults = predictions, isSearching = false) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    }
                }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        _searchQueryFlow.value = newQuery
    }
}
