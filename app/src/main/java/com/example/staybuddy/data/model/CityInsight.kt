package com.example.staybuddy.data.model

import com.google.firebase.Timestamp

/**
 * Pre-computed city market insights — shows on search/listing screens.
 * Updated periodically by admin panel or cloud function.
 */
data class CityInsight(
    val city: String = "",
    val avgRent: Int = 0,
    val minRent: Int = 0,
    val maxRent: Int = 0,
    val totalListings: Int = 0,
    val popularAreas: List<String> = emptyList(),
    val topUniversity: String? = null,
    val lastUpdated: Timestamp? = null
) {
    constructor() : this("")
}
