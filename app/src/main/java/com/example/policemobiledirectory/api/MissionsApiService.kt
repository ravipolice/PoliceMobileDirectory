package com.example.policemobiledirectory.api

import com.example.policemobiledirectory.data.remote.Mission
import com.example.policemobiledirectory.data.remote.MissionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface MissionsApiService {
    @GET("api/missions")
    suspend fun getMissions(): Response<MissionsResponse>

    @POST("api/missions/update")
    suspend fun updateMission(@Body request: UpdateMissionRequest): Response<Unit>
}

data class UpdateMissionRequest(
    val mission: Mission
)

