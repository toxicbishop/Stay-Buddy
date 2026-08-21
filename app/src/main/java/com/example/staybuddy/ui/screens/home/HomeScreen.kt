package com.example.staybuddy.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Close
import com.example.staybuddy.utils.Constants
import com.example.staybuddy.utils.ResponsiveUtils
import com.example.staybuddy.AutoSizeText
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.animation.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.BorderStroke
import com.google.android.gms.location.LocationServices
import com.example.staybuddy.ui.components.PgListingCard
import com.example.staybuddy.ui.components.PromotionBanner
import com.example.staybuddy.ui.components.CityInsightCard
import com.example.staybuddy.ui.components.ListingCardSkeleton
import com.example.staybuddy.ui.components.SectionHeader
import com.example.staybuddy.ui.components.EmptyState
import com.example.staybuddy.ui.components.ErrorBanner
import androidx.compose.material.icons.outlined.TravelExplore
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import android.widget.Toast
import android.Manifest

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.staybuddy.ui.theme.StayBuddyTheme
import com.example.staybuddy.ui.preview.PreviewMockData
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToListingDetail: (String, String) -> Unit,
    onNavigateToRoommates: () -> Unit,
    onNavigateToOwnerDashboard: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        coroutineScope.launch {
            viewModel.loadData()
            // simulate a small delay to show indicator
            delay(500)
            isRefreshing = false
        }
    }

    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.material3.Surface(
                        onClick = { showLocationSheet = true },
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Primary: area/name (big)
                                val primaryText = uiState.selectedArea
                                    ?: uiState.targetAnchor?.name
                                    ?: uiState.selectedCity
                                Text(
                                    text = primaryText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Secondary: city name only (clean, like "Padra" or "Vadodara")
                            val secondaryText = uiState.selectedCity
                            Text(
                                text = secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (showLocationSheet) {
            LocationSelectionBottomSheet(
                uiState = uiState,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onCustomAreaSelected = { 
                    viewModel.updateCustomArea(it)
                    showLocationSheet = false
                },
                onCitySelected = {
                    viewModel.updateCity(it)
                    // Don't close sheet — show areas next
                },
                onAreaSelected = {
                    viewModel.updateArea(it)
                    showLocationSheet = false
                },
                onResetAreas = {
                    viewModel.updateArea(null)
                },
                onUniversitySelected = {
                    viewModel.updateUniversity(it)
                    showLocationSheet = false
                },
                onUseCurrentLocation = {
                    if (locationPermissionState.allPermissionsGranted) {
                        viewModel.refreshLocation()
                        showLocationSheet = false
                    } else {
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                },
                onRecentSelected = {
                    viewModel.updateTargetAnchor(it)
                    showLocationSheet = false
                },
                onDismiss = { showLocationSheet = false }
            )
        }

        // Auto-refresh location if permissions just granted
        LaunchedEffect(locationPermissionState.allPermissionsGranted) {
            if (locationPermissionState.allPermissionsGranted && showLocationSheet) {
                viewModel.refreshLocation()
                showLocationSheet = false
            }
        }

        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = pullRefreshState,
            isRefreshing = isRefreshing || uiState.isLoading,
            onRefresh = onRefresh
        ) {
            // Distance lookup: listingId → distanceKm, built from ViewModel's precomputed list
            // Key includes targetAnchor to force recomputation when location changes
            val distanceMap: Map<String, Double> = remember(uiState.listingsWithDistance, uiState.targetAnchor) {
                uiState.listingsWithDistance
                    .filter { it.second != null }
                    .associate { (listing, dist) -> listing.listingId to dist!! }
            }
            val anchorName = uiState.targetAnchor?.name

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 0.dp) // Bottom padding handled by outer scaffold's innerPadding
            ) {
                // Personalized Greeting with premium feel
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 12.dp)
                    ) {
                        AutoSizeText(
                            text = if (uiState.userRole == Constants.ROLE_OWNER)
                                "Your properties"
                            else if (uiState.userName.isNotEmpty())
                                "Hey ${uiState.userName} 👋"
                            else "Find your next home",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                    }
                }

                // City-specific promotion banner
                item {
                    PromotionBanner(
                        promotion = uiState.activePromotion,
                        onDismiss = { viewModel.dismissPromotion() },
                        onPromotionClick = { /* Deep link handling */ }
                    )
                }

                // Revamped Search Bar - consistent with SearchScreen
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ResponsiveUtils.screenPadding())
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onNavigateToSearch() },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Search PGs, hostels, areas…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Category Chips
                item {
                    val categories = listOf("All", "Full PG", "Shared", "Hostel")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(categories) { index, category ->
                            val isSelected = uiState.selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateCategory(category) },
                                label = {
                                    Text(
                                        text = category,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                    )
                                },
                                shape = MaterialTheme.shapes.small,
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // City market insights
                uiState.cityInsight?.let { insight ->
                    item {
                        CityInsightCard(
                            insight = insight,
                            modifier = Modifier.padding(horizontal = ResponsiveUtils.screenPadding())
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Recommended Section Header
                if (uiState.isLoading && uiState.recommendedListings.isEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recommended",
                            modifier = Modifier.padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 4.dp)
                        )

                        val pagerState = rememberPagerState(pageCount = { 2 })
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListingCardSkeleton(modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else if (uiState.recommendedListings.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recommended",
                            actionLabel = "See all",
                            onAction = onNavigateToSearch,
                            modifier = Modifier.padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 4.dp)
                        )

                        val pagerState = rememberPagerState(pageCount = { uiState.recommendedListings.size })
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val listing = uiState.recommendedListings[page]
                            PgListingCard(
                                listing = listing,
                                isFavorite = uiState.favoriteIds.contains(listing.listingId),
                                onCardClick = { onNavigateToListingDetail(listing.listingId, "recommended") },
                                onFavoriteClick = { viewModel.toggleFavorite(listing.listingId) },
                                distanceKm = distanceMap[listing.listingId],
                                targetAnchor = uiState.targetAnchor,
                                userArea = uiState.userArea,
                                showFreshnessTag = false,
                                cityAvgPrice = uiState.cityInsight?.avgRent ?: 0,
                                origin = "recommended"
                            )
                        }
                    }
                }

                // Nearby Section Header — only show when loading or there are listings
                if (uiState.isLoading || uiState.nearbyListings.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Near you",
                            modifier = Modifier.padding(horizontal = ResponsiveUtils.screenPadding(), vertical = 8.dp)
                        )
                    }
                }

                if (uiState.isLoading && uiState.nearbyListings.isEmpty()) {
                    items(3) {
                        ListingCardSkeleton(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else if (uiState.nearbyListings.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.TravelExplore,
                            title = "Nothing nearby yet",
                            message = "We couldn't find listings around ${uiState.selectedCity}. Try another city or browse everything in search.",
                            actionLabel = "Browse all listings",
                            onAction = onNavigateToSearch
                        )
                    }
                } else {
                    items(uiState.nearbyListings) { listing ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PgListingCard(
                                listing = listing,
                                isFavorite = uiState.favoriteIds.contains(listing.listingId),
                                onCardClick = { onNavigateToListingDetail(listing.listingId, "nearby") },
                                onFavoriteClick = { viewModel.toggleFavorite(listing.listingId) },
                                distanceKm = distanceMap[listing.listingId],
                                targetAnchor = uiState.targetAnchor,
                                userArea = uiState.userArea,
                                showFreshnessTag = false,
                                cityAvgPrice = uiState.cityInsight?.avgRent ?: 0,
                                origin = "nearby",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Conditional CTA based on Role
                if (uiState.userRole != Constants.ROLE_OWNER) {
                    // Roommates CTA for Students
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ResponsiveUtils.screenPadding())
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onNavigateToRoommates() },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(22.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Find your roommate",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "Connect with students who match your lifestyle and habits.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToRoommates,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Explore",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Dashboard CTA for Owners
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = ResponsiveUtils.screenPadding())
                                .clip(MaterialTheme.shapes.large)
                                .clickable { onNavigateToOwnerDashboard() },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(22.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Access your dashboard",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Text(
                                        text = "View listings, manage property status and check new inquiries.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToOwnerDashboard,
                                    modifier = Modifier
                                        .padding(start = 16.dp)
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Dashboard",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Error State
                uiState.error?.let { errorMsg ->
                    item {
                        ErrorBanner(
                            message = errorMsg,
                            onRetry = { viewModel.loadData() },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionBottomSheet(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onCustomAreaSelected: (com.example.staybuddy.domain.model.AutocompletePrediction) -> Unit,
    onCitySelected: (String) -> Unit,
    onAreaSelected: (String?) -> Unit,
    onResetAreas: () -> Unit,
    onUniversitySelected: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRecentSelected: (com.example.staybuddy.domain.model.TargetAnchor) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Select Location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search city, area, or university") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Current Location Button
            Surface(
                onClick = onUseCurrentLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Use Current Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                ) {
                    if (uiState.searchQuery.isNotBlank()) {
                        // ── Show Mapbox Search Results ──
                        if (uiState.isSearching) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.searchResults.isNotEmpty()) {
                            Text(
                                text = "Search Results",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            uiState.searchResults.forEach { prediction ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCustomAreaSelected(prediction) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prediction.primaryText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (prediction.secondaryText.isNotBlank()) {
                                            Text(
                                                text = prediction.secondaryText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No places found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (uiState.availableAreas.isNotEmpty()) {
                        // ── Show areas for selected city ──
                        Text(
                            text = "Areas in ${uiState.selectedCity}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // "All areas" option
                        LocationItem(
                            text = "All ${uiState.selectedCity}",
                            icon = Icons.Default.LocationOn,
                            isSelected = uiState.selectedArea == null,
                            onClick = { onAreaSelected(null) }
                        )

                        uiState.availableAreas.forEach { area ->
                            LocationItem(
                                text = area,
                                icon = Icons.Default.LocationOn,
                                isSelected = area == uiState.selectedArea,
                                onClick = { onAreaSelected(area) }
                            )
                        }

                        // Change city link
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { onResetAreas() }) {
                            Text("← Change City")
                        }
                    } else {
                        // ── Show Recent Locations ──
                        if (uiState.recentLocations.isNotEmpty()) {
                            Text(
                                text = "Recent Locations",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            uiState.recentLocations.forEach { recent ->
                                val icon = when (recent.type) {
                                    com.example.staybuddy.domain.model.AnchorType.UNIVERSITY -> Icons.Default.School
                                    com.example.staybuddy.domain.model.AnchorType.CURRENT_GPS -> Icons.Default.MyLocation
                                    else -> Icons.Default.LocationOn
                                }
                                LocationItem(
                                    text = recent.name,
                                    icon = icon,
                                    isSelected = false,
                                    onClick = { onRecentSelected(recent) }
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // ── Show cities (default view) ──
                        if (uiState.supportedCities.isNotEmpty()) {
                            Text(
                                text = "Popular Cities",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            uiState.supportedCities.forEach { city ->
                                LocationItem(
                                    text = city,
                                    icon = Icons.Default.LocationOn,
                                    isSelected = city == uiState.selectedCity,
                                    onClick = { onCitySelected(city) }
                                )
                            }
                        }

                        if (uiState.supportedUniversities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Top Universities",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            uiState.supportedUniversities.forEach { uni ->
                                LocationItem(
                                    text = uni,
                                    icon = Icons.Default.School,
                                    isSelected = uni == uiState.selectedUniversity,
                                    onClick = { onUniversitySelected(uni) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    StayBuddyTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Column {
                            Text("Hello, Aasav 👋", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Vadodara", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "Search") }
                        IconButton(onClick = {}) { Icon(Icons.Default.Notifications, contentDescription = "Notifications") }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Find your perfect stay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Discover verified PGs near your college", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Text("Nearby PGs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(PreviewMockData.sampleListings) { listing ->
                    PgListingCard(
                        listing = listing,
                        isFavorite = listing.listingId == "listing_001",
                        onFavoriteClick = {},
                        onCardClick = {}
                    )
                }
            }
        }
    }
}
