package com.example.policemobiledirectory.repository

import android.content.Context
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.utils.SecurityConfig
import com.example.policemobiledirectory.model.UnitModel
import com.example.policemobiledirectory.model.UnitMapping
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Admin-specific ConstantsRepository.
 * Extends BaseConstantsRepository and adds CRUD operations.
 */
@Singleton
class ConstantsRepository @Inject constructor(
    @ApplicationContext context: Context,
    apiService: ConstantsApiService,
    securityConfig: SecurityConfig,
    firestore: FirebaseFirestore
) : BaseConstantsRepository(context, apiService, securityConfig, firestore) {

    // --- DISTRICTS ---
    suspend fun addDistrict(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val district = mapOf("name" to name.trim())
            firestore.collection("districts").document(name.trim()).set(district).await()
            clearCache()
            Result.success("District '$name' added successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDistrict(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("districts").document(name.trim()).delete().await()
            clearCache()
            fetchDistrictsFromFirestore()
            Result.success("District '$name' deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDistrict(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val district = mapOf("name" to newName.trim())
            firestore.collection("districts").document(oldName.trim()).delete().await()
            firestore.collection("districts").document(newName.trim()).set(district).await()
            clearCache()
            Result.success("District renamed from '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- STATIONS ---
    suspend fun addStation(district: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val station = mapOf("name" to name.trim(), "district" to district.trim())
            val docId = "${district.trim()}_${name.trim()}"
            firestore.collection("stations").document(docId).set(station).await()
            clearCache()
            Result.success("Station '$name' added to '$district'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStation(district: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docId = "${district.trim()}_${name.trim()}"
            firestore.collection("stations").document(docId).delete().await()
            clearCache()
            fetchStationsFromFirestore()
            Result.success("Station '$name' deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStation(district: String, oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val station = mapOf("name" to newName.trim(), "district" to district.trim())
            val oldDocId = "${district.trim()}_${oldName.trim()}"
            val newDocId = "${district.trim()}_${newName.trim()}"
            firestore.collection("stations").document(oldDocId).delete().await()
            firestore.collection("stations").document(newDocId).set(station).await()
            clearCache()
            Result.success("Station renamed from '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- UNITS ---
    suspend fun addUnit(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val unit = mapOf("name" to name.trim(), "isActive" to true)
            firestore.collection("units").document(name.trim()).set(unit).await()
            clearCache()
            Result.success("Unit '$name' added successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUnit(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("units").document(name.trim()).delete().await()
            clearCache()
            fetchUnitsFromFirestore()
            Result.success("Unit '$name' deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUnit(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val unit = mapOf("name" to newName.trim(), "isActive" to true)
            firestore.collection("units").document(oldName.trim()).delete().await()
            firestore.collection("units").document(newName.trim()).set(unit).await()
            clearCache()
            Result.success("Unit renamed from '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUnitDetails(unit: UnitModel): Result<String> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("units").document(unit.name).set(unit).await()
            clearCache()
            Result.success("Unit details for '${unit.name}' updated successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- RANKS ---
    suspend fun addRank(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rank = mapOf("rank_id" to name.trim(), "isActive" to true)
            firestore.collection("rankMaster").document(name.trim()).set(rank).await()
            clearCache()
            Result.success("Rank '$name' added successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRank(name: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("rankMaster").document(name.trim()).delete().await()
            clearCache()
            fetchRanksFromFirestore()
            Result.success("Rank '$name' deleted successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRank(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rank = mapOf("rank_id" to newName.trim(), "isActive" to true)
            firestore.collection("rankMaster").document(oldName.trim()).delete().await()
            firestore.collection("rankMaster").document(newName.trim()).set(rank).await()
            clearCache()
            Result.success("Rank renamed from '$oldName' to '$newName'")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SECTIONS ---
    suspend fun addSection(unitName: String, sectionName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("unit_sections").document(unitName)
            val doc = docRef.get().await()
            val sections = if (doc.exists()) (doc.get("sections") as? List<String>)?.toMutableList() ?: mutableListOf() else mutableListOf()
            if (!sections.contains(sectionName)) {
                sections.add(sectionName)
                docRef.set(mapOf("sections" to sections)).await()
                Result.success("Section '$sectionName' added to '$unitName'")
            } else {
                Result.success("Section already exists")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSection(unitName: String, sectionName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("unit_sections").document(unitName)
            val doc = docRef.get().await()
            val sections = if (doc.exists()) (doc.get("sections") as? List<String>)?.toMutableList() ?: mutableListOf() else mutableListOf()
            if (sections.remove(sectionName)) {
                docRef.set(mapOf("sections" to sections)).await()
                Result.success("Section '$sectionName' removed from '$unitName'")
            } else {
                Result.success("Section not found")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
 
     // --- SUB-SECTIONS ---
     suspend fun addSubSection(name: String): Result<String> = withContext(Dispatchers.IO) {
         try {
             val docRef = firestore.collection("app_config").document("station_sub_sections")
             val doc = docRef.get().await()
             val items = if (doc.exists()) (doc.get("items") as? List<String>)?.toMutableList() ?: mutableListOf() else mutableListOf()
             if (!items.contains(name.trim())) {
                 items.add(name.trim())
                 docRef.set(mapOf("items" to items)).await()
                 clearCache()
                 Result.success("Sub-section '$name' added successfully")
             } else {
                 Result.success("Sub-section already exists")
             }
         } catch (e: Exception) {
             Result.failure(e)
         }
     }
 
     suspend fun deleteSubSection(name: String): Result<String> = withContext(Dispatchers.IO) {
         try {
             val docRef = firestore.collection("app_config").document("station_sub_sections")
             val doc = docRef.get().await()
             val items = if (doc.exists()) (doc.get("items") as? List<String>)?.toMutableList() ?: mutableListOf() else mutableListOf()
             if (items.remove(name.trim())) {
                 docRef.set(mapOf("items" to items)).await()
                 clearCache()
                 Result.success("Sub-section '$name' deleted successfully")
             } else {
                 Result.success("Sub-section not found")
             }
         } catch (e: Exception) {
             Result.failure(e)
         }
     }
 
     suspend fun updateSubSection(oldName: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
         try {
             val docRef = firestore.collection("app_config").document("station_sub_sections")
             val doc = docRef.get().await()
             val items = if (doc.exists()) (doc.get("items") as? List<String>)?.toMutableList() ?: mutableListOf() else mutableListOf()
             val index = items.indexOf(oldName.trim())
             if (index != -1) {
                 items[index] = newName.trim()
                 docRef.set(mapOf("items" to items)).await()
                 clearCache()
                 Result.success("Sub-section updated from '$oldName' to '$newName'")
             } else {
                 Result.success("Sub-section not found")
             }
         } catch (e: Exception) {
             Result.failure(e)
         }
     }

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

    // --- DUTY ROLE MAPPING ---
    suspend fun updateDutyRoleMapping(unit: String, roles: List<String>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("app_config").document("duty_role_mapping")
            val doc = docRef.get().await()
            val mapping = if (doc.exists()) {
                (doc.get("mapping") as? Map<String, *>)?.mapValues { 
                    (it.value as? List<*>)?.mapNotNull { item -> item?.toString() } ?: emptyList<String>()
                }?.toMutableMap() ?: mutableMapOf()
            } else {
                mutableMapOf()
            }
            
            mapping[unit] = roles
            docRef.set(mapOf("mapping" to mapping)).await()
            clearCache()
            Result.success("Duty roles for unit '$unit' updated successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
