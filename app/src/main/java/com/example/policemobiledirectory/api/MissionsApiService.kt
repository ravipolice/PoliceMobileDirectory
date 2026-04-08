package com.example.policemobiledirectory.api

import com.example.policemobiledirectory.data.remote.MissionsResponse
import retrofit2.Response
import retrofit2.http.GET

interface MissionsApiService {
    @GET("api/missions")
    suspend fun getMissions(): Response<MissionsResponse>
}
