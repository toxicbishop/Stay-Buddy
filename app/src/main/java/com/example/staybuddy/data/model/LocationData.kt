package com.example.staybuddy.data.model

/**
 * Firestore document models for the `location_data` collection.
 * Uses flat Maps (not nested data classes) so Firestore's auto-deserialization works reliably.
 */

/** location_data/cities — city name → coordinates + display ordering */
data class CityDataDocument(
    val cities: Map<String, Map<String, Double>> = emptyMap(),
    val displayOrder: List<String> = emptyList()
)

/** location_data/universities — uni name → coordinates + city affiliation */
data class UniversityDataDocument(
    val universities: Map<String, Map<String, Double>> = emptyMap(),
    val cityAffiliations: Map<String, String> = emptyMap()
)

/** location_data/geofences — city name → polygon vertices (lat/lon maps) */
data class GeofenceDataDocument(
    val areas: Map<String, List<Map<String, Double>>> = emptyMap()
)
