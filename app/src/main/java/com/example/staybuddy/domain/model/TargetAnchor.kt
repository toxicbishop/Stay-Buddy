package com.example.staybuddy.domain.model

enum class AnchorType {
    UNIVERSITY, CUSTOM_AREA, CURRENT_GPS, CITY
}

data class TargetAnchor(
    val type: AnchorType,
    val name: String,
    val lat: Double,
    val lon: Double
)
