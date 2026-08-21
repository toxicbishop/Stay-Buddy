package com.example.staybuddy.data.model

/**
 * A neighborhood/area within a city. Used for proximity-based recommendations.
 * Stored in Firestore: location_data/areas/{areaId}
 */
data class Area(
    val id: String = "",
    val name: String = "",
    val city: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
) {
    constructor() : this("")
}
