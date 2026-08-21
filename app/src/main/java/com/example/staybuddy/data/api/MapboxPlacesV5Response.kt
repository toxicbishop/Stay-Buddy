package com.example.staybuddy.data.api

import com.google.gson.annotations.SerializedName

/**
 * Mapbox Geocoding v5 mapbox.places API response model.
 * Standard, robust response format for forward geocoding in India.
 */
data class MapboxPlacesV5Response(
    val type: String = "",
    val features: List<MapboxV5Feature> = emptyList()
)

data class MapboxV5Feature(
    val id: String = "",
    val text: String = "",
    @SerializedName("place_name") val placeName: String = "",
    val center: List<Double> = emptyList(), // [longitude, latitude]
    val context: List<MapboxV5ContextItem> = emptyList()
) {
    val longitude: Double get() = center.getOrElse(0) { 0.0 }
    val latitude: Double get() = center.getOrElse(1) { 0.0 }
}

data class MapboxV5ContextItem(
    val id: String = "",
    val text: String = ""
)
