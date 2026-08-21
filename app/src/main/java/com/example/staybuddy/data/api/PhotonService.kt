package com.example.staybuddy.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Komoot Photon Geocoding API Retrofit service.
 * Built on OpenStreetMap for instant real-time prefix & fuzzy autocomplete search.
 * Base URL: https://photon.komoot.io/
 */
interface PhotonService {

    @GET("api/")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10,
        @Query("lang") lang: String = "en",
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null
    ): PhotonResponse
}
