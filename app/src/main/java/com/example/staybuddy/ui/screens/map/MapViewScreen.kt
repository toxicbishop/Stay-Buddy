package com.example.staybuddy.ui.screens.map

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.staybuddy.ui.components.MapboxMapView
import com.example.staybuddy.ui.components.map.CompactMapCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapViewScreen(
    onNavigateToListingDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Location permission state
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Trigger to center map on user location
    var myLocationTrigger by remember { mutableIntStateOf(0) }
    
    // Compass variables
    var resetBearingTrigger by remember { mutableIntStateOf(0) }
    var currentBearing by remember { mutableDoubleStateOf(0.0) }
    val showCompass by remember { derivedStateOf { Math.abs(currentBearing) > 1.0 } }

    // Request permission when screen opens
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Let content draw behind system bars if needed
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Full Screen Map
            MapboxMapView(
                modifier = Modifier.fillMaxSize(),
                listings = uiState.listings,
                currentLocation = uiState.userLocation,
                cityCenter = uiState.userLocation ?: uiState.listings.firstOrNull()?.let {
                    com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude)
                },
                selectedListing = uiState.selectedListing,
                myLocationTrigger = myLocationTrigger,
                resetBearingTrigger = resetBearingTrigger,
                onMarkerClick = { listing ->
                    viewModel.selectListing(listing)
                },
                onBearingChanged = { bearing -> currentBearing = bearing }
            )
            
            // Top Action Area (Floating Back Button + Compass)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.TopStart),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Back")
                }

                // Compass Indicator
                AnimatedVisibility(
                    visible = showCompass,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    SmallFloatingActionButton(
                        onClick = { resetBearingTrigger++ },
                        shape = CircleShape,
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
            }

            // Loading / Error / Empty Overlay Indicators
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = paddingValues.calculateTopPadding())
                )
            } else if (uiState.error != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = paddingValues.calculateTopPadding() + 60.dp, start = 16.dp, end = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    com.example.staybuddy.ui.components.ErrorBanner(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadData() }
                    )
                }
            } else if (uiState.listings.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = paddingValues.calculateBottomPadding() + 24.dp, start = 24.dp, end = 24.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No PGs found on map in this area",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Bottom Area (FAB + Pager)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Location FAB
                FloatingActionButton(
                    onClick = {
                        if (!locationPermissionState.status.isGranted) {
                            locationPermissionState.launchPermissionRequest()
                        } else {
                            myLocationTrigger++
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }

                AnimatedVisibility(
                    visible = uiState.listings.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    val pagerState = rememberPagerState(pageCount = { uiState.listings.size })

                    // targetPage tracks the page we're programmatically scrolling to.
                    // settledPage collector skips selectListing when it matches, breaking the loop.
                    var targetPage by remember { mutableIntStateOf(-1) }

                    // Marker click → scroll pager to matching card.
                    LaunchedEffect(uiState.selectedListing) {
                        val selectedId = uiState.selectedListing?.listingId ?: return@LaunchedEffect
                        val index = uiState.listings.indexOfFirst { it.listingId == selectedId }
                        if (index != -1 && pagerState.settledPage != index) {
                            targetPage = index
                            pagerState.animateScrollToPage(index)
                        }
                    }

                    // Manual swipe → update selected listing (and map marker).
                    // If settledPage == targetPage it was our own programmatic scroll — skip.
                    LaunchedEffect(pagerState) {
                        androidx.compose.runtime.snapshotFlow { pagerState.settledPage }.collect { page ->
                            if (page == targetPage) {
                                targetPage = -1   // reset so future manual swipe to same page works
                                return@collect
                            }
                            if (uiState.listings.isNotEmpty() && page < uiState.listings.size) {
                                val swipedListing = uiState.listings[page]
                                if (uiState.selectedListing?.listingId != swipedListing.listingId) {
                                    viewModel.selectListing(swipedListing)
                                }
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        val listing = uiState.listings[page]
                        CompactMapCard(
                            listing = listing,
                            onCardClick = { onNavigateToListingDetail(listing.listingId) },
                            showFavorite = false
                        )
                    }
                }
            }
        }
    }
}
