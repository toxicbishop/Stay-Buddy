package com.example.staybuddy.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mapbox Geocoding API Retrofit service supporting both v5 and v6.
 * Base URL: https://api.mapbox.com/
 */
interface MapboxGeocodingService {

    /**
     * Mapbox v5 mapbox.places Forward Geocoding.
     * Direct, robust endpoint for place & location searches in India.
     */
    @GET("geocoding/v5/mapbox.places/{query}.json")
    suspend fun searchPlacesV5(
        @Path("query") query: String,
        @Query("access_token") accessToken: String,
        @Query("country") country: String = "in",
        @Query("autocomplete") autocomplete: Boolean = true,
        @Query("fuzzyMatch") fuzzyMatch: Boolean = true,
        @Query("limit") limit: Int = 10,
        @Query("proximity") proximity: String? = null,
        @Query("types") types: String? = "poi,place,neighborhood,locality,postcode"
    ): MapboxPlacesV5Response

    /**
     * Forward geocode v6: text query -> list of place features.
     */
    @GET("search/geocode/v6/forward")
    suspend fun forwardGeocode(
        @Query("q") query: String,
        @Query("access_token") accessToken: String,
        @Query("country") country: String = "in",
        @Query("language") language: String = "en",
        @Query("limit") limit: Int = 10,
        @Query("autocomplete") autocomplete: Boolean = true,
        @Query("fuzzy_match") fuzzyMatch: Boolean = true,
        @Query("proximity") proximity: String? = null,
        @Query("types") types: String? = null
    ): MapboxGeocodeResponse

    /**
     * Reverse geocode v6: coordinates -> place details.
     */
    @GET("search/geocode/v6/reverse")
    suspend fun reverseGeocode(
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double,
        @Query("access_token") accessToken: String,
        @Query("language") language: String = "en",
        @Query("types") types: String = "neighborhood,locality,place,address"
    ): MapboxGeocodeResponse
}
