package com.example.ardamapp

import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApi {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,          // "lat,lng"
        @Query("destination") destination: String, // "lat,lng"
        @Query("mode") mode: String,              // "walking" / "driving" / "transit"
        @Query("key") key: String
    ): DirectionsResponse
}