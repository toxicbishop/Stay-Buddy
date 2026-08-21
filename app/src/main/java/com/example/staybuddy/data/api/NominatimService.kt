package com.example.staybuddy.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimService {
    @GET("search")
    suspend fun searchLocation(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countryCodes: String = "in",
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int = 0
    ): List<NominatimResponse>

    @GET("reverse")
    suspend fun reverseLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "jsonv2",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("zoom") zoom: Int = 18
    ): NominatimResponse
}
