package com.example.staybuddy.data.repository

import android.util.Log
import com.example.staybuddy.data.api.MapboxGeocodingService
import com.example.staybuddy.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestore

data class LocationAliasModel(
    val id: String = "",
    val keywords: List<String> = emptyList(),
    val shortName: String = "",
    val fullName: String = "",
    val city: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val isActive: Boolean = true
)

/**
 * 3-Engine Location Service: Mapbox + Nominatim + Photon + Firestore Custom Aliases.
 * All engines run in parallel for maximum coverage and speed.
 * Each engine handles its own errors — no single failure blocks results.
 */
@Singleton
class LocationService @Inject constructor(
    private val mapboxGeocodingService: MapboxGeocodingService,
    private val nominatimService: com.example.staybuddy.data.api.NominatimService,
    private val photonService: com.example.staybuddy.data.api.PhotonService,
    private val locationMappingRepository: LocationMappingRepository,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "LocationService"
    }

    private var cachedAliases = listOf<LocationAliasModel>()

    init {
        // Listen to Firestore location_aliases for live updates
        try {
            firestore.collection("location_aliases").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                cachedAliases = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(LocationAliasModel::class.java)
                }.filter { it.isActive }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize location aliases listener", e)
        }
    }

    /**
     * Reverse geocode: (lat, lon) -> LocationResult.
     * Uses Mapbox Geocoding API.
     */
    suspend fun reverseGeocode(lat: Double, lon: Double): LocationResult? {
        return withContext(Dispatchers.IO) {
            try {
                val response = mapboxGeocodingService.reverseGeocode(
                    longitude = lon,
                    latitude = lat,
                    accessToken = Constants.MAPBOX_ACCESS_TOKEN
                )

                val feature = response.features.firstOrNull() ?: return@withContext null
                val ctx = feature.properties.context

                val city = ctx.place?.name ?: ctx.locality?.name ?: ctx.district?.name ?: ""
                val area = ctx.neighborhood?.name ?: ctx.locality?.name ?: feature.properties.name
                val state = ctx.region?.name ?: ""
                val country = ctx.country?.name ?: "India"
                val rawName = feature.properties.name.ifBlank { feature.properties.placeFormatted }

                LocationResult(
                    city = city,
                    area = area,
                    state = state,
                    country = country,
                    displayName = rawName,
                    lat = lat,
                    lon = lon
                )
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocode failed for ($lat, $lon)", e)
                null
            }
        }
    }

    /**
     * 3-Engine Parallel Autocomplete:
     * - Mapbox Places v5 (best for general places)
     * - Nominatim/OSM (best for Indian villages, institutes)
     * - Photon/OSM (fuzzy matching, good coverage)
     *
     * All run concurrently. Results merged + deduplicated.
     * Each engine is isolated — failure doesn't block others.
     */
    suspend fun autocomplete(query: String, biasLat: Double? = null, biasLon: Double? = null): List<LocationSuggestion> {
        return withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.length < 2) return@withContext emptyList()

            val allSuggestions = java.util.Collections.synchronizedList(mutableListOf<LocationSuggestion>())

            coroutineScope {
                // Engine 1: Mapbox Places v5 (with proximity bias)
                val mapboxJob = async {
                    try {
                        val token = Constants.MAPBOX_ACCESS_TOKEN
                        val proximity = if (biasLat != null && biasLon != null) "$biasLon,$biasLat" else null
                        val response = mapboxGeocodingService.searchPlacesV5(
                            query = cleanQuery,
                            accessToken = token,
                            country = "in",
                            limit = 8,
                            proximity = proximity
                        )

                        response.features.forEach { feat ->
                            val primaryName = feat.text.ifBlank { feat.placeName }
                            val lat = feat.latitude
                            val lon = feat.longitude

                            // India-only: drop fuzzy matches that land outside India
                            val country = feat.context.firstOrNull { it.id.startsWith("country") }?.text ?: ""
                            if (lat != 0.0 && lon != 0.0 && country.contains("india", ignoreCase = true)) {
                                // Prefer the city-level context; fall back to locality/district for POIs
                                val city = feat.context.firstOrNull { it.id.startsWith("place") }?.text
                                    ?: feat.context.firstOrNull { it.id.startsWith("locality") }?.text
                                    ?: feat.context.firstOrNull { it.id.startsWith("district") }?.text
                                    ?: ""
                                val region = feat.context.firstOrNull { it.id.startsWith("region") }?.text ?: ""

                                allSuggestions.add(LocationSuggestion(
                                    displayName = primaryName,
                                    shortName = extractShortName(primaryName),
                                    city = city,
                                    area = buildAreaLabel(city, region),
                                    lat = lat,
                                    lon = lon
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Mapbox v5 error: ${e.message}")
                    }
                }

                // Engine 2: Nominatim/OSM (best for Indian villages, institutes, colleges)
                val nominatimJob = async {
                    try {
                        // When the user's location is known, bound the search to their region
                        // (±1.0° ≈ 110km box). Nominatim ranks nearby matches far better, so
                        // "parul" surfaces Parul University (Vadodara) instead of random West
                        // Bengal villages. Falls back to a global search when no location.
                        val viewbox = if (biasLat != null && biasLon != null) {
                            "${biasLon - 1.0},${biasLat - 1.0},${biasLon + 1.0},${biasLat + 1.0}"
                        } else null
                        val nomResults = nominatimService.searchLocation(
                            query = cleanQuery,
                            limit = 8,
                            countryCodes = "in",
                            viewbox = viewbox,
                            bounded = if (viewbox != null) 1 else 0
                        )

                        nomResults.forEach { res ->
                            val addr = res.address
                            val rawName = res.display_name.split(",").firstOrNull()?.trim() ?: res.display_name
                            // Indian villages/hamlets are labelled "village"/"hamlet" — include them as the city
                            val city = addr?.city ?: addr?.town ?: addr?.village ?: addr?.hamlet ?: addr?.state_district ?: ""
                            val region = addr?.state ?: ""
                            val lat = res.lat.toDoubleOrNull() ?: 0.0
                            val lon = res.lon.toDoubleOrNull() ?: 0.0

                            val country = addr?.country ?: ""
                            if (lat != 0.0 && lon != 0.0 && country.contains("india", ignoreCase = true)) {
                                allSuggestions.add(LocationSuggestion(
                                    displayName = rawName,
                                    shortName = extractShortName(rawName),
                                    city = city,
                                    area = buildAreaLabel(city, region),
                                    lat = lat,
                                    lon = lon
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Nominatim error: ${e.message}")
                    }
                }

                // Engine 3: Photon/OSM (fuzzy matching, good for partial/misspelled queries)
                val photonJob = async {
                    try {
                        val photonResponse = photonService.searchLocation(
                            query = cleanQuery,
                            limit = 8,
                            lat = biasLat,
                            lon = biasLon
                        )

                        photonResponse.features.forEach { feat ->
                            val props = feat.properties
                            val name = props.name ?: props.street ?: props.district ?: ""
                            val city = props.city ?: props.locality ?: props.district ?: ""
                            val region = props.state ?: ""
                            val coords = feat.geometry.coordinates

                            // Photon is global with no country filter — drop international matches (e.g. Kota Kinabalu)
                            val country = props.country ?: ""
                            if (name.isNotBlank() && coords.size >= 2 && country.contains("india", ignoreCase = true)) {
                                val lon = coords[0]
                                val lat = coords[1]

                                allSuggestions.add(LocationSuggestion(
                                    displayName = name,
                                    shortName = extractShortName(name),
                                    city = city,
                                    area = buildAreaLabel(city, region),
                                    lat = lat,
                                    lon = lon
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Photon error: ${e.message}")
                    }
                }

                // Wait for all engines to complete
                mapboxJob.await()
                nominatimJob.await()
                photonJob.await()
            }

            // ── 1. Engine results with custom alias overrides ──────────────
            val engineResults = mutableListOf<LocationSuggestion>()
            allSuggestions.forEach { sugg ->
                val matchingAlias = cachedAliases.firstOrNull { alias ->
                    val fullNameMatch = alias.fullName.isNotBlank() && (
                        alias.fullName.equals(sugg.displayName, ignoreCase = true) ||
                        alias.fullName.contains(sugg.displayName, ignoreCase = true) ||
                        sugg.displayName.contains(alias.fullName, ignoreCase = true)
                    )
                    val keywordMatch = alias.keywords.any { k ->
                        k.isNotBlank() && (
                            sugg.displayName.lowercase().contains(k.lowercase()) ||
                            cleanQuery.lowercase().contains(k.lowercase()) ||
                            k.lowercase().contains(cleanQuery.lowercase())
                        )
                    }
                    fullNameMatch || keywordMatch
                }

                if (matchingAlias != null) {
                    engineResults.add(
                        sugg.copy(
                            shortName = matchingAlias.shortName.ifBlank { extractShortName(sugg.displayName) },
                            displayName = matchingAlias.fullName.ifBlank { sugg.displayName }
                        )
                    )
                } else {
                    engineResults.add(sugg)
                }
            }

            // ── 2. Custom Firestore aliases (highest trust) pinned on top ──
            val matchedCustomAliases = cachedAliases.filter { alias ->
                alias.keywords.any { k -> k.isNotBlank() && (k.contains(cleanQuery, ignoreCase = true) || cleanQuery.contains(k, ignoreCase = true)) } ||
                (alias.fullName.isNotBlank() && alias.fullName.contains(cleanQuery, ignoreCase = true)) ||
                (alias.shortName.isNotBlank() && alias.shortName.contains(cleanQuery, ignoreCase = true))
            }

            val pinned = mutableListOf<LocationSuggestion>()
            matchedCustomAliases.forEach { alias ->
                val exists = engineResults.any { it.shortName.equals(alias.shortName, ignoreCase = true) }
                if (!exists) {
                    pinned.add(
                        LocationSuggestion(
                            displayName = alias.fullName.ifBlank { alias.shortName },
                            shortName = alias.shortName,
                            city = alias.city,
                            area = alias.city,
                            lat = alias.lat,
                            lon = alias.lon
                        )
                    )
                }
            }

            // ── 3. Relevance-rank engine results (fuzzy fallback if none match) ──
            val queryTokens = cleanQuery.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
            val abbrevTokens = queryTokens.filter { it.length <= 4 }

            val rankedEngine = if (queryTokens.isEmpty()) {
                engineResults
            } else {
                val scored = engineResults.map { sugg -> sugg to relevanceScore(sugg, queryTokens, abbrevTokens) }
                val matched = scored.filter { it.second > 0 }
                // If no engine result contains any query token, keep a small fuzzy fallback
                val source = if (matched.isNotEmpty()) matched else scored.take(3)
                source.sortedWith(
                    compareByDescending<Pair<LocationSuggestion, Int>> { it.second }
                        // Equal relevance → nearer to the user wins (Parul University ≈ 20km
                        // beats Parui Mauza, West Bengal ≈ 1700km)
                        .thenBy { (sugg, _) ->
                            if (biasLat != null && biasLon != null) {
                                val dLat = sugg.lat - biasLat
                                val dLon = sugg.lon - biasLon
                                dLat * dLat + dLon * dLon
                            } else 0.0
                        }
                ).map { it.first }
            }

            // ── 4. Pinned aliases first, then ranked engine; dedup ─────────
            (pinned + rankedEngine).distinctBy { it.shortName.lowercase().trim() }
        }
    }

    /**
     * Relevance score for engine results: full-query match ≫ token matches,
     * with a heavy bonus for short abbreviation tokens ("MSU", "IITE") which
     * are usually the discriminating part of a query.
     */
    private fun relevanceScore(
        sugg: LocationSuggestion,
        queryTokens: List<String>,
        abbrevTokens: List<String>
    ): Int {
        val name = sugg.displayName.lowercase()
        val cityArea = "${sugg.city} ${sugg.area}".lowercase()
        val fullQuery = queryTokens.joinToString(" ")
        var score = 0
        if (name.contains(fullQuery) || cityArea.contains(fullQuery)) score += 100
        score += queryTokens.count { name.contains(it) }
        score += queryTokens.count { it.length >= 3 && cityArea.contains(it) }
        score += abbrevTokens.count { name.contains(it) } * 10
        return score
    }

    /**
     * Detect city from GPS coordinates using supported cities list.
     * Falls back to Mapbox if city not in our list.
     */
    suspend fun detectCityFromGps(lat: Double, lon: Double): String? {
        val supportedCity = locationMappingRepository.getNearestSupportedCity(lat, lon)
        if (supportedCity != null) return supportedCity

        val result = reverseGeocode(lat, lon)
        return result?.city
    }

    /**
     * Detect area from GPS coordinates using areas list.
     * Falls back to Mapbox if area not in our list.
     */
    suspend fun detectAreaFromGps(lat: Double, lon: Double, city: String): String? {
        val supportedArea = locationMappingRepository.detectUserArea(lat, lon, city)
        if (supportedArea != null) return supportedArea

        val result = reverseGeocode(lat, lon)
        return result?.area
    }
}

/**
 * Builds a compact "City, Region" label without leading separators when the
 * city is unknown (avoids the old ", Gujarat" artifacts).
 */
fun buildAreaLabel(city: String, region: String): String =
    listOf(city, region).filter { it.isNotBlank() }.distinct().joinToString(", ")

/**
 * Extracts a short/abbreviated name from a full place name.
 * e.g. "Indian Institute Of Teacher Education (IITE)" -> "IITE"
 * Falls back to the original name if no abbreviation is detected.
 */
fun extractShortName(name: String): String {
    // Pattern: "Full Name (ABBREV)" — pick the abbreviation in parentheses
    val parenMatch = Regex("""\(([A-Za-z]{2,10})\)\s*$""").find(name.trim())
    if (parenMatch != null) return parenMatch.groupValues[1]
    return name
}

/**
 * Location result from reverse geocoding.
 */
data class LocationResult(
    val city: String,
    val area: String,
    val state: String,
    val country: String,
    val displayName: String,
    val lat: Double,
    val lon: Double
)

/**
 * Location suggestion for autocomplete.
 */
data class LocationSuggestion(
    val displayName: String,    // Full name e.g. "Indian Institute Of Teacher Education (IITE)"
    val shortName: String = displayName, // Abbreviated name e.g. "IITE"; defaults to displayName
    val city: String,
    val area: String,
    val lat: Double,
    val lon: Double
)
