package com.example.policemobiledirectory.repository

import android.content.Context
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.model.UnitMapping
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-specific ConstantsRepository.
 * Extends BaseConstantsRepository and adds user-side logic if needed.
 */
@Singleton
class ConstantsRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiService: ConstantsApiService,
    securityConfig: SecurityConfig,
    firestore: FirebaseFirestore,
    private val employeeDao: EmployeeDao
) : BaseConstantsRepository(context, apiService, securityConfig, firestore) {

    /**
     * Get stations for a given unit from a list of district stations.
     * Returns empty list for district-level units (e.g. DAR) to hide the Section/Branch field.
     */
    override fun getStationsForUnit(unitName: String, districtStations: List<String>): List<String> {
        // Hide Section/Branch for district-level units (e.g. DAR with "district" scope)
        // Check isDistrictLevel boolean OR scopes containing "district" as fallback
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null)
        if (json != null) {
            try {
                val mappings: Map<String, UnitMapping> = gson.fromJson(
                    json, object : TypeToken<Map<String, UnitMapping>>() {}.type
                )
                val mapping = mappings[unitName]
                val isDistrictLevel = mapping?.isDistrictLevel == true ||
                    mapping?.scopes?.contains("district") == true
                if (isDistrictLevel) {
                    return emptyList()
                }
            } catch (e: Exception) {
                // Fall through to default behavior
            }
        }
        return com.example.policemobiledirectory.utils.Constants.getStationsForUnit(unitName, districtStations)
    }

    // Additional user-specific logic can go here (e.g. clearing local DB on version mismatch)
}
