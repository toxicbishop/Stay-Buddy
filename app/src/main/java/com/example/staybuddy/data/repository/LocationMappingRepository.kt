package com.example.staybuddy.data.repository

import android.util.Log
import com.example.staybuddy.data.model.CityDataDocument
import com.example.staybuddy.data.model.GeofenceDataDocument
import com.example.staybuddy.data.model.UniversityDataDocument
import com.example.staybuddy.utils.Constants
import com.example.staybuddy.utils.LocationUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides city coordinates, university coordinates, and geofence polygons.
 *
 * Data is fetched once from Firestore on [initialize] and cached in memory for the
 * app lifetime (this is a Singleton). If Firestore is unavailable, hardcoded fallback
 * defaults are used silently — callers never see a difference.
 *
 * The synchronous public API ([getCityCoordinates], etc.) stays unchanged so existing
 * callers don't need modification.
 */
@Singleton
class LocationMappingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "LocationMappingRepo"
    }

    // ── Fallback defaults (empty to strictly rely on Firestore) ────

    private val fallbackCities = emptyMap<String, Pair<Double, Double>>()
    private val fallbackGeofencedAreas = emptyMap<String, List<Pair<Double, Double>>>()
    private val fallbackUniversities = emptyMap<String, Pair<Double, Double>>()
    private val fallbackAreas = emptyList<com.example.staybuddy.data.model.Area>()

    // ── In-memory cache (populated from Firestore) ───────────────────

    @Volatile private var cities: Map<String, Pair<Double, Double>> = fallbackCities
    @Volatile private var cityDisplayOrder: List<String> = emptyList()
    @Volatile private var universities: Map<String, Pair<Double, Double>> = fallbackUniversities
    @Volatile private var geofencedAreas: Map<String, List<Pair<Double, Double>>> = fallbackGeofencedAreas
    @Volatile private var areas: List<com.example.staybuddy.data.model.Area> = emptyList()
    @Volatile private var isLoaded = false

    // ── Initialization ──────────────────────────────────────────────────────────

    /**
     * Fetch location data from Firestore and cache it in memory.
     * Safe to call multiple times — subsequent calls refresh the cache.
     * On failure, hardcoded defaults remain intact (no crash, no error propagation).
     */
    suspend fun initialize() {
        try {
            val doc = firestore.collection(Constants.LOCATION_DATA_COLLECTION)
                .document(Constants.CITIES_DOCUMENT)
                .get()
                .await()

            if (doc.exists()) {
                val data = doc.toObject(CityDataDocument::class.java)
                if (data != null && data.cities.isNotEmpty()) {
                    cities = data.cities.mapValues { (_, coords) ->
                        Pair(coords["lat"] ?: 0.0, coords["lon"] ?: 0.0)
                    }
                    cityDisplayOrder = data.displayOrder.ifEmpty { cities.keys.toList() }
                }
            }

            val uniDoc = firestore.collection(Constants.LOCATION_DATA_COLLECTION)
                .document(Constants.UNIVERSITIES_DOCUMENT)
                .get()
                .await()

            if (uniDoc.exists()) {
                val data = uniDoc.toObject(UniversityDataDocument::class.java)
                if (data != null && data.universities.isNotEmpty()) {
                    universities = data.universities.mapValues { (_, coords) ->
                        Pair(coords["lat"] ?: 0.0, coords["lon"] ?: 0.0)
                    }
                }
            }

            val geoDoc = firestore.collection(Constants.LOCATION_DATA_COLLECTION)
                .document(Constants.GEOFENCES_DOCUMENT)
                .get()
                .await()

            if (geoDoc.exists()) {
                val data = geoDoc.toObject(GeofenceDataDocument::class.java)
                if (data != null && data.areas.isNotEmpty()) {
                    geofencedAreas = data.areas.mapValues { (_, vertices) ->
                        vertices.map { v -> Pair(v["lat"] ?: 0.0, v["lon"] ?: 0.0) }
                    }
                }
            }

            // Fetch areas — support both formats:
            // Format 1 (flat): location_data/areas document with "areas" field
            // Format 2 (nested): location_data/cities/{cityName} with "areas" field per city
            val allAreas = mutableListOf<com.example.staybuddy.data.model.Area>()

            // Try flat format first: location_data/areas
            val areasDoc = firestore.collection(Constants.LOCATION_DATA_COLLECTION)
                .document("areas")
                .get()
                .await()
            if (areasDoc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val areasData = areasDoc.data?.get("areas") as? Map<String, Any>
                if (areasData != null) {
                    // Handle nested structure: { "Vadodara": { "dattoura": {...} } }
                    for ((cityName, cityAreas) in areasData) {
                        @Suppress("UNCHECKED_CAST")
                        val cityAreasMap = cityAreas as? Map<String, Map<String, Any>>
                        if (cityAreasMap != null) {
                            for ((areaId, areaData) in cityAreasMap) {
                                allAreas.add(
                                    com.example.staybuddy.data.model.Area(
                                        id = areaId,
                                        name = areaData["name"] as? String ?: areaId,
                                        city = cityName,
                                        lat = (areaData["lat"] as? Number)?.toDouble() ?: 0.0,
                                        lon = (areaData["lon"] as? Number)?.toDouble() ?: 0.0
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Also try nested format: location_data/cities/{cityName} with "areas" field
            if (allAreas.isEmpty()) {
                for ((cityName, _) in cities) {
                    val cityDoc = firestore.collection(Constants.LOCATION_DATA_COLLECTION)
                        .document(Constants.CITIES_DOCUMENT)
                        .collection(cityName)
                        .document("areas")
                        .get()
                        .await()
                    if (cityDoc.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val cityAreasData = cityDoc.data?.get("areas") as? Map<String, Any>
                        if (cityAreasData != null) {
                            for ((areaId, areaData) in cityAreasData) {
                                @Suppress("UNCHECKED_CAST")
                                val areaMap = areaData as? Map<String, Any>
                                if (areaMap != null) {
                                    allAreas.add(
                                        com.example.staybuddy.data.model.Area(
                                            id = areaId,
                                            name = areaMap["name"] as? String ?: areaId,
                                            city = cityName,
                                            lat = (areaMap["lat"] as? Number)?.toDouble() ?: 0.0,
                                            lon = (areaMap["lon"] as? Number)?.toDouble() ?: 0.0
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            areas = allAreas.ifEmpty { fallbackAreas }

            isLoaded = true
            Log.d(TAG, "Location data loaded: ${cities.size} cities, ${universities.size} unis, ${areas.size} areas")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore fetch failed, using hardcoded defaults", e)
        }
    }

    // ── Public API (synchronous — unchanged signature) ───────────────────────────

    fun getCityCoordinates(cityName: String): Pair<Double, Double>? = cities[cityName]

    fun getUniversityCoordinates(uniName: String): Pair<Double, Double>? = universities[uniName]

    fun getGeofencedArea(lat: Double, lon: Double): String? {
        for ((area, polygon) in geofencedAreas) {
            if (LocationUtils.isPointInPolygon(lat, lon, polygon)) {
                return area
            }
        }
        return null
    }

    fun getNearestSupportedCity(lat: Double, lon: Double, thresholdKm: Double = 30.0): String? {
        var closestCity: String? = null
        var minDistance = Double.MAX_VALUE
        for ((city, coords) in cities) {
            val dist = LocationUtils.calculateDistance(lat, lon, coords.first, coords.second)
            if (dist < minDistance) {
                minDistance = dist
                closestCity = city
            }
        }
        return if (minDistance <= thresholdKm) closestCity else null
    }

    // ── New helpers for UI ───────────────────────────────────────────────────────

    /** Returns city names in display order (Firestore order, or fallback alphabetical). */
    fun getSupportedCityNames(): List<String> = cityDisplayOrder

    /** Returns all supported university names. */
    fun getSupportedUniversityNames(): List<String> = universities.keys.toList()

    /**
     * Detect which area/neighborhood the user is in based on GPS coordinates.
     * Returns the nearest area name within the same city, or null if no areas defined.
     */
    fun detectUserArea(lat: Double, lon: Double, city: String): String? {
        val cityAreas = areas.filter { it.city.equals(city, ignoreCase = true) }
        if (cityAreas.isEmpty()) return null

        var nearestArea: String? = null
        var minDist = Double.MAX_VALUE

        for (area in cityAreas) {
            val dist = LocationUtils.calculateDistance(lat, lon, area.lat, area.lon)
            if (dist < minDist) {
                minDist = dist
                nearestArea = area.name
            }
        }
        return nearestArea
    }

    /** Get all areas for a specific city. */
    fun getAreasForCity(city: String): List<com.example.staybuddy.data.model.Area> {
        return areas.filter { it.city.equals(city, ignoreCase = true) }
    }
}
