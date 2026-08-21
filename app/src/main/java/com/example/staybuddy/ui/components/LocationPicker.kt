package com.example.staybuddy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun LocationPicker(
    initialLocation: Point? = null,
    onLocationSelected: (Point) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationPickerViewModel = hiltViewModel()
) {
    var isExpanded by remember { mutableStateOf(false) }

    if (isExpanded) {
        Dialog(
            onDismissRequest = { isExpanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                LocationPickerContent(
                    initialLocation = initialLocation,
                    onLocationSelected = {
                        onLocationSelected(it)
                        isExpanded = false
                    },
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    isExpanded = true,
                    onToggleExpand = { isExpanded = false }
                )
            }
        }
    } else {
        LocationPickerContent(
            initialLocation = initialLocation,
            onLocationSelected = onLocationSelected,
            modifier = modifier,
            viewModel = viewModel,
            isExpanded = false,
            onToggleExpand = { isExpanded = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
private fun LocationPickerContent(
    initialLocation: Point?,
    onLocationSelected: (Point) -> Unit,
    modifier: Modifier,
    viewModel: LocationPickerViewModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val defaultIndiaCenter = Point.fromLngLat(78.9629, 20.5937)
    val startPoint = initialLocation ?: defaultIndiaCenter
    val isDarkMapTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

    var currentCenter by remember { mutableStateOf(startPoint) }
    var isDragging by remember { mutableStateOf(false) }
    var mapboxMapRef by remember { mutableStateOf<com.mapbox.maps.MapboxMap?>(null) }

    // If initialLocation is provided, update currentCenter
    LaunchedEffect(initialLocation) {
        if (initialLocation != null) {
            currentCenter = initialLocation
        }
    }

    // Auto-detect current GPS location if no initial location provided
    LaunchedEffect(initialLocation, mapboxMapRef) {
        if (initialLocation == null && mapboxMapRef != null) {
            val loc = viewModel.getCurrentLocation()
            if (loc != null) {
                viewModel.setCurrentLocation(loc.first, loc.second)
                val detectedPoint = Point.fromLngLat(loc.second, loc.first)
                currentCenter = detectedPoint
                onLocationSelected(detectedPoint)
                mapboxMapRef?.flyTo(
                    CameraOptions.Builder()
                        .center(detectedPoint)
                        .zoom(14.0)
                        .build(),
                    MapAnimationOptions.mapAnimationOptions { duration(800L) }
                )
            }
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }

    val pinOffset by animateFloatAsState(
        targetValue = if (isDragging) -24f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "pinOffset"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Mapbox Map View
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setBackgroundColor(surfaceColor)
                    val mapboxMap = getMapboxMap()
                    mapboxMapRef = mapboxMap

                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(startPoint)
                            .zoom(if (initialLocation != null) 15.0 else 5.0)
                            .build()
                    )
                    mapboxMap.loadStyle(if (isDarkMapTheme) Style.DARK else Style.MAPBOX_STREETS)

                    compass.enabled = false
                    logo.enabled = false
                    attribution.enabled = false
                    scalebar.enabled = false

                    mapboxMap.subscribeCameraChanged {
                        currentCenter = mapboxMap.cameraState.center
                    }

                    // Fix for scroll interception in Compose scrollable views
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                view.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                view.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }

                    gestures.addOnMoveListener(object : OnMoveListener {
                        override fun onMoveBegin(detector: MoveGestureDetector) {
                            isDragging = true
                            searchActive = false
                        }
                        override fun onMove(detector: MoveGestureDetector): Boolean = false
                        override fun onMoveEnd(detector: MoveGestureDetector) {
                            isDragging = false
                        }
                    })
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Pin
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = pinOffset.dp)
            ) {
                AnimatedVisibility(visible = isDragging) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = MaterialTheme.shapes.small,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            "Drag map to adjust pin",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Selected Pin Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Top Search Bar & Suggestions Container (ALWAYS VISIBLE for instant search)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(8.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Docked Search Input
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search Location",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                viewModel.onSearchQueryChange(query)
                                searchActive = query.trim().isNotEmpty()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search location (e.g. Parul University...)",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchActive = false
                                    viewModel.clearSearch()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Fullscreen Expand/Collapse Toggle Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 4.dp,
                    onClick = onToggleExpand
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isExpanded) "Collapse Map" else "Expand Map",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Live Autocomplete Suggestions Dropdown
            if (searchActive && suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    val isLoadingSuggestions by viewModel.isLoading.collectAsStateWithLifecycle()
                    if (isLoadingSuggestions) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            // Show short/abbreviated name (e.g. "IITE") as headline
                                            suggestion.shortName,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        // If abbreviation was extracted, show full name first, then area
                                        val hasAbbrev = suggestion.shortName != suggestion.displayName
                                        val locationSubtext = listOf(
                                            if (hasAbbrev) suggestion.displayName else "",
                                            suggestion.area, suggestion.city
                                        ).filter { it.isNotBlank() && !it.equals(suggestion.shortName, ignoreCase = true) }
                                         .distinct().joinToString(", ")
                                        if (locationSubtext.isNotBlank()) {
                                            Text(
                                                locationSubtext,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            // 1. Hide suggestions dropdown & update search box with short name
                                            searchActive = false
                                            viewModel.onSearchQueryChange(suggestion.shortName)
                                            
                                            // 2. Create target point & update currentCenter immediately
                                            val selectedPoint = Point.fromLngLat(suggestion.lon, suggestion.lat)
                                            currentCenter = selectedPoint

                                            // 3. Immediately notify parent form of selected point
                                            onLocationSelected(selectedPoint)

                                            // 4. Smoothly fly map camera to selected point
                                            mapboxMapRef?.flyTo(
                                                CameraOptions.Builder()
                                                    .center(selectedPoint)
                                                    .zoom(16.0)
                                                    .build(),
                                                MapAnimationOptions.mapAnimationOptions { duration(800L) }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls (My Location FAB + Confirm Location Button)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val locationPermissionState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
                permissions = listOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            // Current GPS Location FAB
            FloatingActionButton(
                onClick = {
                    if (locationPermissionState.allPermissionsGranted) {
                        coroutineScope.launch {
                            val loc = viewModel.getCurrentLocation()
                            if (loc != null) {
                                viewModel.setCurrentLocation(loc.first, loc.second)
                                val userGpsPoint = Point.fromLngLat(loc.second, loc.first)
                                currentCenter = userGpsPoint
                                onLocationSelected(userGpsPoint)
                                mapboxMapRef?.flyTo(
                                    CameraOptions.Builder()
                                        .center(userGpsPoint)
                                        .zoom(16.0)
                                        .bearing(0.0)
                                        .pitch(0.0)
                                        .build(),
                                    MapAnimationOptions.mapAnimationOptions { duration(800L) }
                                )
                            } else {
                                android.widget.Toast.makeText(context, "Could not detect current location", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        locationPermissionState.launchMultiplePermissionRequest()
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Detect My Location")
            }

            // Confirm / Select Location Button
            Button(
                onClick = { onLocationSelected(currentCenter) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDragging,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Confirm Location", fontWeight = FontWeight.Bold)
            }
        }
    }
}
