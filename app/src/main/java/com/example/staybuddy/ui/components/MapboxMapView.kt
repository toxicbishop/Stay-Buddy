package com.example.staybuddy.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme
import com.example.staybuddy.data.model.PgListing
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.toNumber
import com.mapbox.maps.extension.style.expressions.dsl.generated.toString
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.scalebar.scalebar

private const val LISTING_SOURCE_ID = "listing-source"
private const val LISTING_LAYER_ID = "listing-layer"
private const val CLUSTER_CIRCLE_LAYER_ID = "cluster-circle-layer"
private const val CLUSTER_COUNT_LAYER_ID = "cluster-count-layer"
private const val LOCATION_SOURCE_ID = "location-source"
private const val LOCATION_LAYER_ID = "location-layer"

/**
 * Mapbox-powered map view with smooth flyTo camera transitions, listing markers,
 * GeoJSON-based clustering, and current-location indicator.
 */
@Composable
fun MapboxMapView(
    modifier: Modifier = Modifier,
    listings: List<PgListing> = emptyList(),
    currentLocation: Point? = null,
    cityCenter: Point? = null,
    selectedListing: PgListing? = null,
    isPickerMode: Boolean = false,
    myLocationTrigger: Int = 0,
    resetBearingTrigger: Int = 0,
    onLocationSelected: (Point) -> Unit = {},
    onMarkerClick: (PgListing) -> Unit = {},
    onBearingChanged: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkMapTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Theme colors for markers
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    val onTertiaryColor = MaterialTheme.colorScheme.onTertiary.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

    // Track previous values to avoid redundant camera moves
    var lastSelectedListingId by remember { mutableStateOf<String?>(null) }
    var lastMyLocationTrigger by remember { mutableIntStateOf(0) }
    var lastResetBearingTrigger by remember { mutableIntStateOf(0) }
    var hasSetInitialCamera by remember { mutableStateOf(false) }
    var styleLoaded by remember { mutableStateOf(false) }

    // Keep a live reference — factory {} captures these once, so we need State objects
    // that the click listener can read .value on to always get the latest data.
    val listingsById = remember(listings) { listings.associateBy { it.listingId } }
    val listingsByIdState = rememberUpdatedState(listingsById)
    val onMarkerClickState = rememberUpdatedState(onMarkerClick)

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setBackgroundColor(surfaceColor)
                    // Disable default Mapbox UI elements
                    compass.enabled = false
                    logo.enabled = false
                    attribution.enabled = false
                    scalebar.enabled = false

                    // Enable pitch (tilt) gesture — two-finger drag up/down for 3D view
                    gestures.pitchEnabled = true
                    gestures.simultaneousRotateAndPinchToZoomEnabled = true

                    // Set initial camera with 55° pitch & 16.5 zoom so 3D buildings show clearly
                    val startPoint = currentLocation ?: cityCenter ?: Point.fromLngLat(73.1812, 22.3072)
                    getMapboxMap().setCamera(
                        CameraOptions.Builder()
                            .center(startPoint)
                            .zoom(16.5)
                            .pitch(55.0)
                            .bearing(0.0)
                            .build()
                    )

                    // Load custom StayBuddy Mapbox Studio styles
                    val hearthNightStyle = "mapbox://styles/arctixchauhan/cmsgfo59g00ik01sfheivatf6"
                    val hearthDayStyle = "mapbox://styles/arctixchauhan/cmsgfoaio00jy01s82am17pwa"
                    getMapboxMap().loadStyle(if (isDarkMapTheme) hearthNightStyle else hearthDayStyle) { style ->

                        // Listing source with clustering enabled
                        style.addSource(geoJsonSource(LISTING_SOURCE_ID) {
                            featureCollection(FeatureCollection.fromFeatures(emptyList()))
                            cluster(true)
                            clusterRadius(50)
                            clusterMaxZoom(14)
                        })

                        // Location source (no clustering)
                        style.addSource(geoJsonSource(LOCATION_SOURCE_ID) {
                            featureCollection(FeatureCollection.fromFeatures(emptyList()))
                        })

                        // --- Cluster circle layer ---
                        style.addLayer(circleLayer(CLUSTER_CIRCLE_LAYER_ID, LISTING_SOURCE_ID) {
                            filter(has("point_count"))
                            // Size: scales with cluster count
                            circleRadius(
                                Expression.step(
                                    get("point_count"),
                                    literal(20.0),   // base radius
                                    literal(10.0), literal(25.0),  // 10+ items
                                    literal(30.0), literal(30.0)   // 30+ items
                                )
                            )
                            circleColor(primaryColor)
                            circleOpacity(0.85)
                            circleStrokeWidth(3.0)
                            circleStrokeColor(surfaceColor)
                        })

                        // --- Cluster count text layer ---
                        style.addLayer(symbolLayer(CLUSTER_COUNT_LAYER_ID, LISTING_SOURCE_ID) {
                            filter(has("point_count"))
                            textField(Expression.toString(get("point_count")))
                            textSize(13.0)
                            textColor(onPrimaryColor)
                            textFont(listOf("DIN Pro Medium", "Arial Unicode MS Bold"))
                            textAllowOverlap(true)
                            textIgnorePlacement(true)
                        })

                        // --- Individual (unclustered) listing markers ---
                        style.addLayer(symbolLayer(LISTING_LAYER_ID, LISTING_SOURCE_ID) {
                            filter(
                                Expression.not(has("point_count"))
                            )
                            iconImage(get("icon-id"))
                            iconAllowOverlap(true)
                            iconIgnorePlacement(true)
                            iconAnchor(com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor.BOTTOM)
                            iconSize(1.0)
                        })

                        // --- Location blue dot layer ---
                        style.addLayer(symbolLayer(LOCATION_LAYER_ID, LOCATION_SOURCE_ID) {
                            iconImage("blue-dot")
                            iconAllowOverlap(true)
                            iconIgnorePlacement(true)
                            iconAnchor(com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor.CENTER)
                            iconSize(1.0)
                        })

                        // Add the blue dot image to style
                        val blueDot = createBlueDotBitmap(surfaceColor, primaryColor)
                        style.addImage("blue-dot", blueDot)

                        styleLoaded = true
                    }

                    // Enable smooth gestures
                    gestures.apply {
                        rotateEnabled = true
                        pitchEnabled = true
                        scrollEnabled = true
                        scrollDecelerationEnabled = true
                    }

                    // Report bearing changes for compass indicator (throttled)
                    var lastReportedBearing = 0.0
                    getMapboxMap().subscribeCameraChanged {
                        val bearing = getMapboxMap().cameraState.bearing
                        if (Math.abs(bearing - lastReportedBearing) > 5.0) {
                            lastReportedBearing = bearing
                            onBearingChanged(bearing)
                        }
                    }

                    // Handle map clicks
                    getMapboxMap().addOnMapClickListener { point ->
                        val pixel = getMapboxMap().pixelForCoordinate(point)
                        // 25px radius (50×50px box) — tight enough to distinguish nearby pins
                        val screenBox = com.mapbox.maps.ScreenBox(
                            com.mapbox.maps.ScreenCoordinate(pixel.x - 25.0, pixel.y - 25.0),
                            com.mapbox.maps.ScreenCoordinate(pixel.x + 25.0, pixel.y + 25.0)
                        )
                        val geometry = com.mapbox.maps.RenderedQueryGeometry(screenBox)

                        // First check cluster clicks
                        getMapboxMap().queryRenderedFeatures(
                            geometry,
                            com.mapbox.maps.RenderedQueryOptions(listOf(CLUSTER_CIRCLE_LAYER_ID), null)
                        ) { expected ->
                            val clusterFeature = expected.value?.firstOrNull()
                            if (clusterFeature != null) {
                                // Zoom into the cluster
                                val clusterPoint = clusterFeature.queriedFeature.feature.geometry() as? Point
                                clusterPoint?.let { cp ->
                                    val currentZoom = getMapboxMap().cameraState.zoom
                                    getMapboxMap().flyTo(
                                        CameraOptions.Builder()
                                            .center(cp)
                                            .zoom(currentZoom + 2.0)
                                            .build(),
                                        MapAnimationOptions.mapAnimationOptions {
                                            duration(600L)
                                        }
                                    )
                                }
                            } else {
                                // Check individual marker clicks — pick the pin closest to the
                                // actual tap point (not just firstOrNull which uses render z-order)
                                getMapboxMap().queryRenderedFeatures(
                                    geometry,
                                    com.mapbox.maps.RenderedQueryOptions(listOf(LISTING_LAYER_ID), null)
                                ) { markerExpected ->
                                    val candidates = markerExpected.value ?: return@queryRenderedFeatures
                                    // Find closest marker to the tap in screen-pixel space
                                    val best = candidates.minByOrNull { qf ->
                                        val geoPt = qf.queriedFeature.feature.geometry() as? Point
                                        if (geoPt != null) {
                                            val fp = getMapboxMap().pixelForCoordinate(geoPt)
                                            val dx = fp.x - pixel.x
                                            val dy = fp.y - pixel.y
                                            dx * dx + dy * dy
                                        } else Double.MAX_VALUE
                                    }
                                    best?.let { queriedFeature ->
                                        val listingId = queriedFeature.queriedFeature.feature.getStringProperty("listing-id")
                                        listingsByIdState.value[listingId]?.let { listing ->
                                            onMarkerClickState.value(listing)
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            onReset = { mapView ->
                mapView.onStop()
            },
            update = { mapView ->
                if (!styleLoaded) return@AndroidView

                val mapboxMap = mapView.getMapboxMap()
                val style = mapboxMap.style ?: return@AndroidView

                // --- Update listing markers ---
                val features = listings.mapNotNull { listing ->
                    if (listing.latitude == 0.0 && listing.longitude == 0.0) return@mapNotNull null
                    val isSelected = selectedListing?.listingId == listing.listingId
                    val priceText = formatPriceShort(listing.price)
                    val imageId = "marker-${listing.listingId}-${isSelected}"

                    if (style.hasStyleImage(imageId) == false) {
                        val markerBitmap = createPriceMarkerBitmap(
                            text = priceText,
                            bgColor = if (isSelected) tertiaryColor else primaryColor,
                            textColor = if (isSelected) onTertiaryColor else onPrimaryColor,
                            isSelected = isSelected
                        )
                        style.addImage(imageId, markerBitmap)
                    }

                    Feature.fromGeometry(
                        Point.fromLngLat(listing.longitude, listing.latitude)
                    ).apply {
                        addStringProperty("listing-id", listing.listingId)
                        addStringProperty("icon-id", imageId)
                    }
                }

                // Update the GeoJSON source with new features
                style.getSourceAs<GeoJsonSource>(LISTING_SOURCE_ID)?.featureCollection(
                    FeatureCollection.fromFeatures(features)
                )

                // --- Update current location blue dot ---
                currentLocation?.let { loc ->
                    style.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID)?.featureCollection(
                        FeatureCollection.fromFeatures(
                            listOf(Feature.fromGeometry(loc))
                        )
                    )
                }

                // --- Smooth camera: fly to selected listing ---
                val currentSelectedId = selectedListing?.listingId
                if (currentSelectedId != null && currentSelectedId != lastSelectedListingId) {
                    lastSelectedListingId = currentSelectedId
                    selectedListing.let { listing ->
                        if (listing.latitude != 0.0 && listing.longitude != 0.0) {
                            mapboxMap.flyTo(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(listing.longitude, listing.latitude))
                                    .zoom(16.5)
                                    .pitch(55.0)
                                    .build(),
                                MapAnimationOptions.mapAnimationOptions {
                                    duration(800L)
                                }
                            )
                        }
                    }
                } else if (currentSelectedId == null && lastSelectedListingId != null) {
                    lastSelectedListingId = null
                }

                // --- Smooth camera: fly to current location on trigger ---
                if (myLocationTrigger != lastMyLocationTrigger) {
                    lastMyLocationTrigger = myLocationTrigger
                    currentLocation?.let { loc ->
                        mapboxMap.flyTo(
                            CameraOptions.Builder()
                                .center(loc)
                                .zoom(16.5)
                                .pitch(55.0)
                                .build(),
                            MapAnimationOptions.mapAnimationOptions {
                                duration(1000L)
                            }
                        )
                    }
                }

                // --- Reset bearing/pitch on trigger ---
                if (resetBearingTrigger != lastResetBearingTrigger) {
                    lastResetBearingTrigger = resetBearingTrigger
                    mapboxMap.flyTo(
                        CameraOptions.Builder()
                            .bearing(0.0)
                            .pitch(55.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(500L)
                        }
                    )
                }

                // --- Initial camera setup (only once) ---
                if (!hasSetInitialCamera && listings.isNotEmpty()) {
                    hasSetInitialCamera = true
                    val center = cityCenter ?: currentLocation
                        ?: listings.firstOrNull()?.let {
                            Point.fromLngLat(it.longitude, it.latitude)
                        }
                    center?.let {
                        mapboxMap.flyTo(
                            CameraOptions.Builder()
                                .center(it)
                                .zoom(16.5)
                                .pitch(55.0)
                                .build(),
                            MapAnimationOptions.mapAnimationOptions {
                                duration(600L)
                            }
                        )
                    }
                }
            }
        )
    }
}

/**
 * Creates a price tag bitmap marker for a listing.
 */
private fun createPriceMarkerBitmap(
    text: String,
    bgColor: Int,
    textColor: Int,
    isSelected: Boolean
): Bitmap {
    val density = 2.5f
    val paddingH = (12 * density).toInt()
    val paddingV = (6 * density).toInt()
    val textSize = if (isSelected) 13f * density else 12f * density
    val cornerRadius = 20f * density
    val arrowHeight = (6 * density).toInt()

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val textWidth = textPaint.measureText(text).toInt()
    val textHeight = (textPaint.descent() - textPaint.ascent()).toInt()

    val bubbleWidth = textWidth + paddingH * 2
    val bubbleHeight = textHeight + paddingV * 2
    val totalHeight = bubbleHeight + arrowHeight

    val bitmap = Bitmap.createBitmap(bubbleWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background pill
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
    }
    canvas.drawRoundRect(
        RectF(0f, 0f, bubbleWidth.toFloat(), bubbleHeight.toFloat()),
        cornerRadius, cornerRadius,
        bgPaint
    )

    // Arrow triangle at bottom center
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val arrowPath = android.graphics.Path().apply {
        moveTo(bubbleWidth / 2f - arrowHeight, bubbleHeight.toFloat())
        lineTo(bubbleWidth / 2f, totalHeight.toFloat())
        lineTo(bubbleWidth / 2f + arrowHeight, bubbleHeight.toFloat())
        close()
    }
    canvas.drawPath(arrowPath, arrowPaint)

    // Price text
    canvas.drawText(
        text,
        bubbleWidth / 2f,
        paddingV.toFloat() - textPaint.ascent(),
        textPaint
    )

    return bitmap
}

/**
 * Creates a blue dot bitmap for current location.
 */
private fun createBlueDotBitmap(outerColor: Int, innerColor: Int): Bitmap {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outerColor
        setShadowLayer(4f, 0f, 2f, 0x30000000)
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, outerPaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerColor
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8f, innerPaint)

    return bitmap
}

/**
 * Formats price into short display form: ₹5K, ₹12K, ₹1.2L
 */
private fun formatPriceShort(price: Int): String {
    return when {
        price >= 100000 -> "₹${String.format("%.1f", price / 100000.0)}L"
        price >= 1000 -> "₹${price / 1000}K"
        else -> "₹$price"
    }
}
