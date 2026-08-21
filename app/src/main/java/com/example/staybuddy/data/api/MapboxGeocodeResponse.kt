package com.example.staybuddy.data.api

import com.google.gson.annotations.SerializedName

/**
 * Mapbox Geocoding API v6 response wrapper.
 * Used for both forward and reverse geocoding.
 */
data class MapboxGeocodeResponse(
    val type: String = "",
    val features: List<MapboxFeature> = emptyList()
)

data class MapboxFeature(
    val type: String = "",
    val geometry: MapboxGeometry = MapboxGeometry(),
    val properties: MapboxProperties = MapboxProperties()
)

data class MapboxGeometry(
    val type: String = "",
    val coordinates: List<Double> = emptyList() // [longitude, latitude]
) {
    val longitude: Double get() = coordinates.getOrElse(0) { 0.0 }
    val latitude: Double get() = coordinates.getOrElse(1) { 0.0 }
}

data class MapboxProperties(
    val name: String = "",
    @SerializedName("mapbox_id") val mapboxId: String = "",
    @SerializedName("feature_type") val featureType: String = "",
    @SerializedName("place_formatted") val placeFormatted: String = "",
    @SerializedName("full_address") val fullAddress: String = "",
    val context: MapboxContext = MapboxContext()
)

data class MapboxContext(
    val neighborhood: MapboxContextItem? = null,
    val locality: MapboxContextItem? = null,
    val place: MapboxContextItem? = null,
    val district: MapboxContextItem? = null,
    val region: MapboxContextItem? = null,
    val country: MapboxContextItem? = null
)

data class MapboxContextItem(
    val name: String = "",
    @SerializedName("mapbox_id") val mapboxId: String = ""
)
