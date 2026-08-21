package com.example.staybuddy.data.api

data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList()
)

data class PhotonFeature(
    val geometry: PhotonGeometry,
    val properties: PhotonProperties
)

data class PhotonGeometry(
    val coordinates: List<Double> = emptyList() // [lon, lat]
)

data class PhotonProperties(
    val osm_id: Long? = null,
    val name: String? = null,
    val street: String? = null,
    val locality: String? = null,
    val district: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postcode: String? = null
)
