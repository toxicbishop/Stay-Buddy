package com.example.staybuddy.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.staybuddy.domain.model.AutocompletePrediction
import com.example.staybuddy.ui.components.MapboxMapView
import com.example.staybuddy.ui.components.PgListingCard
import com.example.staybuddy.ui.components.SaveSearchDialog
import com.example.staybuddy.ui.components.SavedSearchesSheet
import com.example.staybuddy.ui.components.map.CompactMapCard
import com.example.staybuddy.data.model.SavedSearch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.staybuddy.utils.ResponsiveUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToListingDetail: (String) -> Unit,
    onNavigateToMapView: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    savedSearchViewModel: SavedSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val savedSearchState by savedSearchViewModel.uiState.collectAsStateWithLifecycle()

    // Pending save: stores the snapshot until the user names it
    var pendingSaveSearch by remember { mutableStateOf<SavedSearch?>(null) }

    SearchScreenContent(
        uiState = uiState,
        favoriteIds = favoriteIds,
        savedSearchState = savedSearchState,
        onNavigateToListingDetail = onNavigateToListingDetail,
        onNavigateBack = onNavigateBack,
        onSelectListing = viewModel::selectListing,
        onToggleFavorite = viewModel::toggleFavorite,
        onQueryChange = viewModel::onQueryChange,
        onToggleFilterSheet = viewModel::toggleFilterSheet,
        onClearSearchHistory = viewModel::clearSearchHistory,
        onSelectLocation = viewModel::selectLocation,
        onRemoveFromHistory = viewModel::removeFromHistory,
        onToggleMapView = viewModel::toggleMapView,
        onUpdatePriceRange = viewModel::updatePriceRange,
        onToggleRoomType = viewModel::toggleRoomType,
        onSetGenderFilter = viewModel::setGenderFilter,
        onToggleAmenity = viewModel::toggleAmenity,
        onUpdateMaxDistance = viewModel::updateMaxDistance,
        onUpdateSortOption = viewModel::updateSortOption,
        onClearFilters = viewModel::clearFilters,
        onOpenSaveDialog = {
            pendingSaveSearch = viewModel.getCurrentFiltersForSave()
            savedSearchViewModel.showSaveDialog(true)
        },
        onOpenSavedSearches = { savedSearchViewModel.showListSheet(true) },
        onSelectSavedSearch = { saved ->
            viewModel.applySavedSearch(saved)
            savedSearchViewModel.showListSheet(false)
        },
        onDeleteSavedSearch = savedSearchViewModel::deleteSearch,
        onToggleNotify = savedSearchViewModel::toggleNotification
    )

    // Save Search Dialog
    if (savedSearchState.showSaveDialog && pendingSaveSearch != null) {
        SaveSearchDialog(
            currentSearch = pendingSaveSearch!!,
            onDismiss = {
                savedSearchViewModel.showSaveDialog(false)
                pendingSaveSearch = null
            },
            onSave = { search ->
                savedSearchViewModel.saveSearch(search)
                pendingSaveSearch = null
            }
        )
    }

    // Saved Searches List
    if (savedSearchState.showListSheet) {
        SavedSearchesSheet(
            savedSearches = savedSearchState.savedSearches,
            onDismiss = { savedSearchViewModel.showListSheet(false) },
            onSelect = {
                viewModel.applySavedSearch(it)
                savedSearchViewModel.showListSheet(false)
            },
            onDelete = savedSearchViewModel::deleteSearch,
            onToggleNotification = savedSearchViewModel::toggleNotification
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    favoriteIds: Set<String> = emptySet(),
    savedSearchState: SavedSearchUiState = SavedSearchUiState(),
    onNavigateToListingDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onSelectListing: (com.example.staybuddy.data.model.PgListing) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleFilterSheet: (Boolean) -> Unit,
    onClearSearchHistory: () -> Unit,
    onSelectLocation: (AutocompletePrediction) -> Unit,
    onRemoveFromHistory: (AutocompletePrediction) -> Unit,
    onToggleMapView: (Boolean) -> Unit,
    onUpdatePriceRange: (ClosedFloatingPointRange<Float>) -> Unit,
    onToggleRoomType: (String) -> Unit,
    onSetGenderFilter: (String) -> Unit,
    onToggleAmenity: (String) -> Unit,
    onUpdateMaxDistance: (Float) -> Unit,
    onUpdateSortOption: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onOpenSaveDialog: () -> Unit = {},
    onOpenSavedSearches: () -> Unit = {},
    onSelectSavedSearch: (SavedSearch) -> Unit = {},
    onDeleteSavedSearch: (String) -> Unit = {},
    onToggleNotify: (String, Boolean) -> Unit = { _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val screenPadding = ResponsiveUtils.screenPadding()

    // Map controls state
    var myLocationTrigger by remember { mutableIntStateOf(0) }
    var resetBearingTrigger by remember { mutableIntStateOf(0) }
    var currentBearing by remember { mutableDoubleStateOf(0.0) }
    val showCompass by remember { derivedStateOf { Math.abs(currentBearing) > 1.0 } }
    
    // Permission for location
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // Background Layer: Map or List
                if (uiState.isLoading) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 112.dp, bottom = 88.dp, start = screenPadding, end = screenPadding),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(3) {
                            com.example.staybuddy.ui.components.ListingCardSkeleton()
                        }
                    }
                } else if (uiState.isMapView) {
                    // MAP VIEW
                    Box(modifier = Modifier.fillMaxSize()) {
                        MapboxMapView(
                            modifier = Modifier.fillMaxSize(),
                            listings = uiState.filteredListings,
                            cityCenter = uiState.selectedLocationContext?.let {
                                com.mapbox.geojson.Point.fromLngLat(it.lon, it.lat)
                            } ?: uiState.userAnchor?.takeIf { it.lat != 0.0 && it.lon != 0.0 }?.let {
                                com.mapbox.geojson.Point.fromLngLat(it.lon, it.lat)
                            },
                            selectedListing = uiState.selectedListing,
                            myLocationTrigger = myLocationTrigger,
                            resetBearingTrigger = resetBearingTrigger,
                            onMarkerClick = onSelectListing,
                            onBearingChanged = { bearing ->
                                if (Math.abs(bearing - currentBearing) > 5.0) {
                                    currentBearing = bearing
                                }
                            }
                        )

                        // Map Controls (Compass & My Location)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 80.dp, end = 16.dp), // below search bar
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AnimatedVisibility(
                                visible = showCompass,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                SmallFloatingActionButton(
                                    onClick = { resetBearingTrigger++ },
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Explore,
                                        contentDescription = "Reset Bearing",
                                        modifier = Modifier.graphicsLayer { rotationZ = -currentBearing.toFloat() }
                                    )
                                }
                            }
                            
                            SmallFloatingActionButton(
                                onClick = {
                                    if (!locationPermissionState.status.isGranted) {
                                        locationPermissionState.launchPermissionRequest()
                                    } else {
                                        myLocationTrigger++
                                    }
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                            }
                        }

                        // Map Carousel
                        if (uiState.filteredListings.isNotEmpty()) {
                            val pagerState = rememberPagerState(pageCount = { uiState.filteredListings.size })
                            
                            LaunchedEffect(uiState.selectedListing) {
                                uiState.selectedListing?.let { selected ->
                                    val index = uiState.filteredListings.indexOfFirst { it.listingId == selected.listingId }
                                    if (index != -1 && pagerState.currentPage != index) {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            }

                            LaunchedEffect(pagerState.currentPage) {
                                if (uiState.filteredListings.isNotEmpty()) {
                                    onSelectListing(uiState.filteredListings[pagerState.currentPage])
                                }
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 80.dp) // Leave space for Floating Toggle
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                pageSpacing = 16.dp
                            ) { page ->
                                val listing = uiState.filteredListings[page]
                                CompactMapCard(
                                    listing = listing,
                                    onCardClick = { onNavigateToListingDetail(listing.listingId) },
                                    onFavoriteClick = { onToggleFavorite(listing.listingId) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    // LIST VIEW
                    if (uiState.filteredListings.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            com.example.staybuddy.ui.components.EmptyState(
                                icon = Icons.Default.FilterList,
                                title = "No stays match",
                                message = "Try widening the price range or removing a few filters.",
                                actionLabel = "Adjust filters",
                                onAction = { onToggleFilterSheet(true) }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 112.dp, bottom = 88.dp)
                        ) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = screenPadding, vertical = 8.dp)) {
                                    Text(
                                        text = "${uiState.filteredListings.size} ${if (uiState.filteredListings.size == 1) "stay" else "stays"} found",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "sorted by ${uiState.sortOption.displayName.lowercase()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(uiState.filteredListings, key = { it.listingId }) { listing ->
                                PgListingCard(
                                    listing = listing,
                                    isFavorite = favoriteIds.contains(listing.listingId),
                                    onCardClick = { onNavigateToListingDetail(listing.listingId) },
                                    onFavoriteClick = { onToggleFavorite(listing.listingId) },
                                    origin = "search",
                                    modifier = Modifier.padding(horizontal = screenPadding, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Foreground layer: Floating Search Bar + Suggestions dropdown
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    // Gradient backdrop behind search bar only
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (uiState.isMapView)
                                    androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                                else
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), Color.Transparent)
                                    )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(bottom = 4.dp)
                        ) {
                            // Search Pill
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = screenPadding, vertical = 4.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shadowElevation = 6.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = onNavigateBack) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = uiState.query,
                                            onValueChange = onQueryChange,
                                            singleLine = true,
                                            modifier = Modifier.fillMaxSize(),
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            decorationBox = { innerTextField ->
                                                TextFieldDefaults.DecorationBox(
                                                    value = uiState.query,
                                                    innerTextField = innerTextField,
                                                    enabled = true,
                                                    singleLine = true,
                                                    visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                    placeholder = {
                                                        Text(
                                                            "Search PGs, hostels...",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        if (uiState.query.isNotEmpty()) {
                                                            IconButton(onClick = { onQueryChange("") }) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Close,
                                                                    contentDescription = "Clear",
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    },
                                                    colors = TextFieldDefaults.colors(
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        disabledContainerColor = Color.Transparent,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                    ),
                                                    contentPadding = PaddingValues(0.dp)
                                                )
                                            }
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(32.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )

                                    val activeFilterCount = remember(uiState) {
                                        var count = 0
                                        if (uiState.priceRange.start > 500f || uiState.priceRange.endInclusive < 30000f) count++
                                        if (uiState.selectedRoomTypes.isNotEmpty()) count += uiState.selectedRoomTypes.size
                                        if (uiState.selectedGender != "Any") count++
                                        if (uiState.selectedAmenities.isNotEmpty()) count += uiState.selectedAmenities.size
                                        if (uiState.maxDistanceKm < 50f) count++
                                        count
                                    }

                                    IconButton(
                                        onClick = { onToggleFilterSheet(true) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        if (activeFilterCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    ) {
                                                        Text(activeFilterCount.toString())
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FilterList,
                                                    contentDescription = "Filter",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "Filter",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Offline Banner
                            if (uiState.isOffline) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "You're offline. Showing cached results.",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Suggestions dropdown (appears only when typing) ──
                    val query = uiState.query
                    val hasQuery = query.isNotEmpty()
                    val filteredRecent = if (hasQuery) {
                        uiState.recentSearches.filter {
                            it.primaryText.contains(query, ignoreCase = true)
                        }
                    } else emptyList()
                    val predictions = uiState.locationPredictions
                    val showSuggestions = hasQuery && (predictions.isNotEmpty() || filteredRecent.isNotEmpty())

                    AnimatedVisibility(
                        visible = showSuggestions,
                        enter = fadeIn(initialAlpha = 0.6f) + slideInVertically(initialOffsetY = { -it / 4 }),
                        exit = fadeOut(targetAlpha = 0.4f) + slideOutVertically(targetOffsetY = { -it / 4 })
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = screenPadding),
                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 4.dp)
                            ) {
                                // Location predictions
                                if (predictions.isNotEmpty()) {
                                    predictions.take(5).forEach { prediction ->
                                        LocationSuggestionItem(
                                            prediction = prediction,
                                            icon = Icons.Default.LocationOn,
                                            onClick = {
                                                onSelectLocation(prediction)
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                }

                                // Divider between sections
                                if (predictions.isNotEmpty() && filteredRecent.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }

                                // Filtered recent searches
                                if (filteredRecent.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = onClearSearchHistory,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Clear all", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    filteredRecent.take(3).forEach { prediction ->
                                        LocationSuggestionItem(
                                            prediction = prediction,
                                            icon = Icons.Default.History,
                                            onClick = {
                                                onSelectLocation(prediction)
                                                focusManager.clearFocus()
                                            },
                                            onRemove = { onRemoveFromHistory(prediction) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Foreground layer: Floating Toggle Map/List Button
                FloatingToggleButton(
                    isMapView = uiState.isMapView,
                    onToggle = onToggleMapView,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }

            // Filter Bottom Sheet
            if (uiState.isFilterSheetVisible) {
                FilterBottomSheet(
                    uiState = uiState,
                    onDismiss = { onToggleFilterSheet(false) },
                    onPriceChange = onUpdatePriceRange,
                    onRoomTypeToggle = onToggleRoomType,
                    onGenderChange = onSetGenderFilter,
                    onAmenityToggle = onToggleAmenity,
                    onDistanceChange = onUpdateMaxDistance,
                    onSortOrderChange = onUpdateSortOption,
                    onClearFilters = onClearFilters,
                    onSaveSearch = onOpenSaveDialog
                )
            }
        }
    )
}

@Composable
fun FloatingToggleButton(
    isMapView: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .clickable { onToggle(!isMapView) }
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isMapView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.Map,
                contentDescription = if (isMapView) "List View" else "Map View",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isMapView) "List" else "Map",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    uiState: SearchUiState,
    onDismiss: () -> Unit,
    onPriceChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onRoomTypeToggle: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAmenityToggle: (String) -> Unit,
    onDistanceChange: (Float) -> Unit,
    onSortOrderChange: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onSaveSearch: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Refine Search",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = onClearFilters) {
                    Text(
                        text = "Reset all",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(20.dp))

            // 1. Sort By — 2x2 grid so options never squeeze or wrap awkwardly
            FilterSectionHeader("Sort By", icon = Icons.AutoMirrored.Filled.Sort)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption.values().toList().chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowOptions.forEach { order ->
                            val isSelected = uiState.sortOption == order
                            SelectableChipCard(
                                text = order.displayName,
                                isSelected = isSelected,
                                onClick = { onSortOrderChange(order) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Price Range — clear formatted badge
            FilterSectionHeader(
                title = "Price Range",
                value = "₹${com.example.staybuddy.ui.components.formatInr(uiState.priceRange.start.toInt())} – ${com.example.staybuddy.ui.components.formatInr(uiState.priceRange.endInclusive.toInt())}/mo",
                icon = Icons.Default.CurrencyRupee
            )
            RangeSlider(
                value = uiState.priceRange,
                onValueChange = onPriceChange,
                valueRange = 500f..30000f,
                steps = 59,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Gender Preference — rich icons (Any, Male, Female)
            FilterSectionHeader("Gender Preference", icon = Icons.Default.People)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val genders = listOf(
                    Triple("Any", Icons.Default.People, "Any"),
                    Triple("Male", Icons.Default.Male, "Male"),
                    Triple("Female", Icons.Default.Female, "Female")
                )
                genders.forEach { (label, icon, value) ->
                    val isSelected = uiState.selectedGender == value
                    SelectableChipCard(
                        text = label,
                        icon = icon,
                        isSelected = isSelected,
                        onClick = { onGenderChange(value) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Room Selection — 2 per row
            FilterSectionHeader("Room Selection", icon = Icons.Default.Bed)
            val roomTypes = listOf(
                "Single" to "Single Room",
                "Double" to "Double Sharing",
                "Triple" to "Triple Sharing",
                "Dorm" to "Dormitory"
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                roomTypes.chunked(2).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTypes.forEach { (typeKey, label) ->
                            val isSelected = uiState.selectedRoomTypes.contains(typeKey)
                            SelectableChipCard(
                                text = label,
                                isSelected = isSelected,
                                onClick = { onRoomTypeToggle(typeKey) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Amenities — 2 per row with icons so text never wraps tightly
            FilterSectionHeader("Essential Amenities", icon = Icons.Default.CheckCircle)
            val amenityItems = listOf(
                "WiFi" to Icons.Default.Wifi,
                "AC" to Icons.Default.AcUnit,
                "Food" to Icons.Default.Restaurant,
                "Laundry" to Icons.Default.LocalLaundryService,
                "Power Backup" to Icons.Default.ElectricalServices,
                "Gym" to Icons.Default.FitnessCenter
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                amenityItems.chunked(2).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        chunk.forEach { (amenity, icon) ->
                            val isSelected = uiState.selectedAmenities.contains(amenity)
                            SelectableChipCard(
                                text = amenity,
                                icon = icon,
                                isSelected = isSelected,
                                onClick = { onAmenityToggle(amenity) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Max Distance
            FilterSectionHeader(
                title = "Max Distance",
                value = "${uiState.maxDistanceKm.toInt()} km radius",
                icon = Icons.Default.Straighten
            )
            Slider(
                value = uiState.maxDistanceKm,
                onValueChange = onDistanceChange,
                valueRange = 1f..15f,
                steps = 14,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    thumbColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom CTA row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onSaveSearch(); onDismiss() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Save Search", fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1.4f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Show Results", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun FilterSectionHeader(
    title: String,
    value: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (value != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun SelectableChipCard(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LocationSuggestionItem(
    prediction: AutocompletePrediction,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.LocationOn,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prediction.primaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (prediction.secondaryText.isNotEmpty()) {
                Text(
                    text = prediction.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove from history",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AmenityFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    MaterialTheme {
        SearchScreenContent(
            uiState = SearchUiState(),
            onNavigateToListingDetail = {},
            onNavigateBack = {},
            onSelectListing = {},
            onToggleFavorite = {},
            onQueryChange = {},
            onToggleFilterSheet = {},
            onClearSearchHistory = {},
            onSelectLocation = {},
            onRemoveFromHistory = {},
            onToggleMapView = {},
            onUpdatePriceRange = {},
            onToggleRoomType = {},
            onSetGenderFilter = {},
            onToggleAmenity = {},
            onUpdateMaxDistance = {},
            onUpdateSortOption = {},
            onClearFilters = {}
        )
    }
}
