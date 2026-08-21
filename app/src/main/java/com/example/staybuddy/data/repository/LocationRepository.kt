package com.example.staybuddy.data.repository

import com.example.staybuddy.data.api.MapboxGeocodingService
import com.example.staybuddy.data.api.NominatimService
import com.example.staybuddy.domain.model.AutocompletePrediction
import com.example.staybuddy.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.example.staybuddy.data.api.PhotonService

import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class LocationRepository @Inject constructor(
    private val mapboxGeocodingService: MapboxGeocodingService,
    private val nominatimService: NominatimService,
    private val photonService: PhotonService,
    private val fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
) {
    @Volatile
    private var cachedLocation: Pair<Double, Double>? = null

    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        var location: android.location.Location? = null
        try {
            // First try to get a fresh location
            location = com.google.android.gms.tasks.Tasks.await(
                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 
                    null
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (location == null) {
            try {
                // Fallback to last known location if fresh location fails or throws
                location = com.google.android.gms.tasks.Tasks.await(fusedLocationClient.lastLocation)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val res = location?.let { it.latitude to it.longitude }
        if (res != null) cachedLocation = res
        res
    }

    @Suppress("MissingPermission")
    private fun getLastLocationQuick(): Pair<Double, Double>? {
        cachedLocation?.let { return it }
        return try {
            val loc = com.google.android.gms.tasks.Tasks.await(
                fusedLocationClient.lastLocation,
                300,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            if (loc != null) (loc.latitude to loc.longitude).also { cachedLocation = it } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchLocation(query: String): Result<List<AutocompletePrediction>> = withContext(Dispatchers.IO) {
        try {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            coroutineScope {
                val currentLoc = getLastLocationQuick()
                val proximityStr = currentLoc?.let { "${it.second},${it.first}" }

                val searchLat = currentLoc?.first ?: 20.5937
                val searchLon = currentLoc?.second ?: 78.9629

                val photonDeferred = async {
                    withTimeoutOrNull(1200) {
                        try {
                            val response = photonService.searchLocation(
                                query = trimmedQuery,
                                limit = 10,
                                lat = searchLat,
                                lon = searchLon
                            )
                            response.features.mapNotNull { feature ->
                                val props = feature.properties
                                val primary = props.name ?: return@mapNotNull null
                                val secondary = listOfNotNull(
                                    props.street,
                                    props.locality,
                                    props.district ?: props.city,
                                    props.state
                                ).filter { it.isNotBlank() && !it.equals(primary, ignoreCase = true) }
                                    .distinct()
                                    .joinToString(", ")

                                val coords = feature.geometry.coordinates
                                if (coords.size < 2) return@mapNotNull null

                                AutocompletePrediction(
                                    placeId = (props.osm_id ?: primary.hashCode().toLong()),
                                    primaryText = primary,
                                    secondaryText = secondary,
                                    lat = coords[1],
                                    lon = coords[0]
                                )
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()
                }

                val mapboxDeferred = async {
                    withTimeoutOrNull(2000) {
                        try {
                            val response = mapboxGeocodingService.searchPlacesV5(
                                query = trimmedQuery,
                                accessToken = Constants.MAPBOX_ACCESS_TOKEN,
                                country = "in",
                                limit = 10
                            )
                            response.features.mapIndexed { idx, feature ->
                                val primary = feature.text.ifBlank { feature.placeName }
                                val secondary = feature.placeName

                                AutocompletePrediction(
                                    placeId = (feature.id.hashCode().toLong() + idx),
                                    primaryText = primary,
                                    secondaryText = secondary,
                                    lat = feature.latitude,
                                    lon = feature.longitude
                                )
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } ?: emptyList()
                }

                val photonResults = photonDeferred.await()
                val mapboxResults = mapboxDeferred.await()

                val normQuery = trimmedQuery.lowercase()
                val queryWords = normQuery.split(" ").filter { it.isNotBlank() }

                val combined = (photonResults + mapboxResults)
                    .filter { prediction ->
                        // Strictly geofence to India bounding box (Lat: 6.0 N to 37.0 N, Lon: 68.0 E to 97.5 E)
                        prediction.lat in 6.0..37.0 && prediction.lon in 68.0..97.5
                    }
                    .map { prediction ->
                        prediction.copy(
                            primaryText = prediction.primaryText.toTitleCase(),
                            secondaryText = prediction.secondaryText.toTitleCase()
                        )
                    }
                    .distinctBy { normalizePlaceKey(it.primaryText) }
                    .sortedByDescending { prediction ->
                        val normPrimary = prediction.primaryText.lowercase().trim()
                        val normSecondary = prediction.secondaryText.lowercase().trim()

                        var score = when {
                            normPrimary == normQuery -> 100
                            normPrimary.startsWith(normQuery) -> 80
                            queryWords.all { word -> normPrimary.contains(word) } -> 60
                            normPrimary.contains(normQuery) -> 40
                            queryWords.any { word -> normPrimary.startsWith(word) } -> 30
                            normSecondary.startsWith(normQuery) -> 20
                            queryWords.any { word -> normSecondary.contains(word) } -> 10
                            else -> 0
                        }

                        // Apply proximity boost: Nearby places in user's city/area get higher score
                        if (currentLoc != null) {
                            val distKm = calculateDistance(currentLoc.first, currentLoc.second, prediction.lat, prediction.lon)
                            if (distKm <= 15.0) {
                                score += 50 // Same city / neighborhood boost!
                            } else if (distKm <= 50.0) {
                                score += 30 // Nearby metro area boost!
                            } else if (distKm <= 150.0) {
                                score += 15 // Same region boost!
                            }
                        } else if (normSecondary.contains("gujarat")) {
                            score += 15
                        }

                        score
                    }

                Result.success(combined)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun getReverseGeocode(lat: Double, lon: Double): Result<AutocompletePrediction> = withContext(Dispatchers.IO) {
        try {
            val response = mapboxGeocodingService.reverseGeocode(
                longitude = lon,
                latitude = lat,
                accessToken = Constants.MAPBOX_ACCESS_TOKEN
            )

            val feature = response.features.firstOrNull()
            if (feature == null) {
                return@withContext Result.success(
                    AutocompletePrediction(
                        placeId = 0L,
                        primaryText = "Current Location",
                        secondaryText = "",
                        lat = lat,
                        lon = lon
                    )
                )
            }

            val props = feature.properties
            // For reverse geocode, prefer neighborhood/locality for primary text
            val primary = props.context.neighborhood?.name
                ?: props.context.locality?.name
                ?: props.name.ifBlank { "Current Location" }

            val secondaryParts = listOfNotNull(
                props.context.place?.name,
                props.context.district?.name,
                props.context.region?.name
            ).distinct()
            val secondary = secondaryParts.joinToString(", ")

            val prediction = AutocompletePrediction(
                placeId = props.mapboxId.hashCode().toLong(),
                primaryText = primary,
                secondaryText = secondary,
                lat = feature.geometry.latitude,
                lon = feature.geometry.longitude
            )
            Result.success(prediction)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun String.toTitleCase(): String {
        if (this.isBlank()) return this
        return this.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun normalizePlaceKey(text: String): String {
        return text.lowercase()
            .replace("h", "")
            .replace(" ", "")
            .replace("-", "")
            .replace(".", "")
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
