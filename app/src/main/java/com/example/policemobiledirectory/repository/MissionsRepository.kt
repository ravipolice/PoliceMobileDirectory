package com.example.policemobiledirectory.repository

import com.example.policemobiledirectory.api.MissionsApiService
import com.example.policemobiledirectory.data.remote.Mission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionsRepository @Inject constructor(
    private val apiService: MissionsApiService
) {
    private var cachedMissions: List<Mission>? = null

    fun getMissions(forceRefresh: Boolean = false): Flow<Result<List<Mission>>> = flow {
        if (!forceRefresh && cachedMissions != null) {
            emit(Result.success(cachedMissions!!))
            return@flow
        }

        try {
            val response = apiService.getMissions()
            if (response.isSuccessful && response.body() != null) {
                val missions = response.body()!!.data
                cachedMissions = missions
                emit(Result.success(missions))
            } else {
                emit(Result.failure(Exception("Failed to fetch missions: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun updateMission(mission: Mission): Result<Unit> {
        return try {
            val response = apiService.updateMission(com.example.policemobiledirectory.api.UpdateMissionRequest(mission))
            if (response.isSuccessful) {
                // Clear cache on update to force fresh fetch
                cachedMissions = null
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update mission: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

