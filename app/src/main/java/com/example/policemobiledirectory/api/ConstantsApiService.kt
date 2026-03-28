package com.example.policemobiledirectory.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit API Interface
interface ConstantsApiService {

    // 🔹 Fetch constants (GET) - matches PRD format
    @GET("exec")
    suspend fun getConstants(
        @Query("token") token: String? = null
    ): ConstantsApiResponse
}
