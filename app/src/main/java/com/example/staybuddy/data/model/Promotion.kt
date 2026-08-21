package com.example.staybuddy.data.model

import com.google.firebase.Timestamp

/**
 * A city-specific contextual promotion — shows as a subtle banner on the home screen.
 * Managed via admin panel. Type determines where/how it renders.
 *
 * Types: "new_listings" | "seasonal" | "city_launch" | "tip" | "price_drop"
 */
data class Promotion(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val type: String = "tip",
    val icon: String = "💡",
    val city: String = "all",
    val university: String? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val isActive: Boolean = true,
    val priority: Int = 0,
    val deepLink: String? = null,
    val createdAt: Timestamp? = null
) {
    constructor() : this("")
}
