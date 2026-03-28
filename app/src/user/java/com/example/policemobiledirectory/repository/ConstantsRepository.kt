package com.example.policemobiledirectory.repository

import android.content.Context
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.data.local.EmployeeDao
import com.example.policemobiledirectory.utils.SecurityConfig
import com.google.firebase.firestore.FirebaseFirestore
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
     * Uses Constants.getStationsForUnit logic.
     */
    override fun getStationsForUnit(unitName: String, districtStations: List<String>): List<String> {
        return com.example.policemobiledirectory.utils.Constants.getStationsForUnit(unitName, districtStations)
    }

    // Additional user-specific logic can go here (e.g. clearing local DB on version mismatch)
}
