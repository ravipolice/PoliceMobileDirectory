package com.example.policemobiledirectory.repository

import android.content.Context
import android.util.Log
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.api.ConstantsApiService
import com.example.policemobiledirectory.api.ConstantsData
import com.example.policemobiledirectory.utils.SecurityConfig
import com.example.policemobiledirectory.model.UnitModel
import com.example.policemobiledirectory.model.UnitMapping
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * BaseConstantsRepository - Abstract class for managing dynamic constants synchronization.
 * Shared between user and admin flavors.
 */
abstract class BaseConstantsRepository(
    protected val context: Context,
    protected val apiService: ConstantsApiService,
    protected val securityConfig: SecurityConfig,
    protected val firestore: FirebaseFirestore
) {
    protected val prefs = context.getSharedPreferences("constants_cache", Context.MODE_PRIVATE)
    protected val gson = Gson()
    
    // Cache constants
    protected val CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1)
    protected val CACHE_KEY = "remote_constants"
    protected val CACHE_TIMESTAMP_KEY = "cache_timestamp"
    protected val UNITS_CACHE_KEY = "units_cache"
    protected val FULL_UNITS_CACHE_KEY = "full_units_cache"
    protected val DISTRICTS_CACHE_KEY = "districts_cache"
    protected val DISTRICT_SHORT_CODES_CACHE_KEY = "district_short_codes_cache"
    protected val RANKS_CACHE_KEY = "ranks_cache"
    protected val RANKS_METAL_CACHE_KEY = "ranks_requiring_metal_cache"
    protected val STATIONS_CACHE_KEY = "stations_cache"
    protected val GLOBAL_CONFIG_CACHE_KEY = "global_config_cache"
    protected val UNIT_MAPPINGS_CACHE_KEY = "unit_mappings_cache"
    protected val SUB_SECTIONS_CACHE_KEY = "sub_sections_cache"
    protected val DUTY_ROLE_MAPPING_CACHE_KEY = "duty_role_mapping_cache"

    // Global Config State
    protected val _globalHiddenFields = MutableStateFlow<List<String>>(emptyList())
    val globalHiddenFields: StateFlow<List<String>> = _globalHiddenFields.asStateFlow()

    init {
        loadGlobalConfigFromCache()
    }

    /**
     * Check if cache needs refresh
     */
    fun shouldRefreshCache(): Boolean {
        val cachedVersion = prefs.getInt("local_constants_version", 0)
        if (cachedVersion != Constants.LOCAL_CONSTANTS_VERSION) {
            Log.d("BaseConstantsRepo", "⚠️ Local constants version mismatch. Invalidating cache.")
            clearCache()
            prefs.edit().putInt("local_constants_version", Constants.LOCAL_CONSTANTS_VERSION).apply()
            return true
        }

        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return true
        
        if (!prefs.contains(STATIONS_CACHE_KEY)) return true
        
        val age = System.currentTimeMillis() - timestamp
        return age >= CACHE_EXPIRY_MS
    }

    /**
     * Clear the cache
     */
    fun clearCache() {
        prefs.edit()
            .remove(CACHE_KEY)
            .remove(CACHE_TIMESTAMP_KEY)
            .remove(UNITS_CACHE_KEY)
            .remove(FULL_UNITS_CACHE_KEY)
            .remove(DISTRICTS_CACHE_KEY)
            .remove(DISTRICT_SHORT_CODES_CACHE_KEY)
            .remove(RANKS_CACHE_KEY)
            .remove(RANKS_METAL_CACHE_KEY)
            .remove(STATIONS_CACHE_KEY)
            .remove(UNIT_MAPPINGS_CACHE_KEY)
            .remove(SUB_SECTIONS_CACHE_KEY)
            .remove(DUTY_ROLE_MAPPING_CACHE_KEY)
            .apply()
        Log.d("BaseConstantsRepo", "✅ Cache cleared")
    }

    /**
     * Refresh all constants from Firestore
     */
    suspend fun refreshConstants(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                fetchUnitsFromFirestore()
                fetchDistrictsFromFirestore()
                fetchRanksFromFirestore()
                fetchStationsFromFirestore()
                fetchGlobalConfig()
                fetchSubSectionsFromFirestore()
                
                prefs.edit().putLong(CACHE_TIMESTAMP_KEY, System.currentTimeMillis()).apply()
                true
            } else {
                Log.w("BaseConstantsRepo", "⚠️ Skip sync: Not authenticated")
                false
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "❌ Failed to refresh constants: ${e.message}", e)
            false
        }
    }

    protected suspend fun fetchUnitsFromFirestore() {
        try {
            val snapshot = firestore.collection("units")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val unitNames = snapshot.documents.mapNotNull { it.getString("name") }.distinct().sorted()
            
            val unitModels = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val legacyDistricts = (doc.get("mappedDistricts") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val areaIds = (doc.get("mappedAreaIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                
                UnitModel(
                    id = doc.id,
                    name = name,
                    isActive = doc.getBoolean("isActive") ?: true,
                    mappingType = doc.getString("mappingType") ?: "all",
                    mappedDistricts = (legacyDistricts + areaIds).distinct(),
                    isDistrictLevel = doc.getBoolean("isDistrictLevel") ?: false,
                    isHqLevel = doc.getBoolean("isHqLevel") ?: false,
                    scopes = (doc.get("scopes") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    applicableRanks = (doc.get("applicableRanks") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    stationKeyword = doc.getString("stationKeyword") ?: "",
                    hideFromRegistration = doc.getBoolean("hideFromRegistration") ?: false,
                    hiddenFields = (doc.get("hiddenFields") as? List<String>) ?: emptyList(),
                    dutyRoles = (doc.get("dutyRoles") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                )
            }.sortedBy { it.name }

            val mappings = unitModels.associateBy { it.name }.mapValues { (_, model) ->
                UnitMapping(
                    unitName = model.name,
                    mappingType = model.mappingType,
                    mappedDistricts = model.mappedDistricts,
                    isDistrictLevel = model.isDistrictLevel,
                    isHqLevel = model.isHqLevel,
                    scopes = model.scopes,
                    applicableRanks = model.applicableRanks,
                    stationKeyword = model.stationKeyword,
                    mappedAreaType = "" // Default
                )
            }

            if (unitNames.isNotEmpty()) {
                prefs.edit()
                    .putString(UNITS_CACHE_KEY, gson.toJson(unitNames))
                    .putString(FULL_UNITS_CACHE_KEY, gson.toJson(unitModels))
                    .putString(UNIT_MAPPINGS_CACHE_KEY, gson.toJson(mappings))
                    .apply()
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "Error fetching units", e)
        }
    }

    protected suspend fun fetchDistrictsFromFirestore() {
        try {
            val snapshot = firestore.collection("districts").get().await()
            val districtNames = snapshot.documents.mapNotNull { it.getString("name") }.distinct().sorted()
            val shortCodesMap = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val code = doc.getString("shortCode") ?: return@mapNotNull null
                name to code
            }.toMap()
            if (districtNames.isNotEmpty()) {
                prefs.edit()
                    .putString(DISTRICTS_CACHE_KEY, gson.toJson(districtNames))
                    .putString(DISTRICT_SHORT_CODES_CACHE_KEY, gson.toJson(shortCodesMap))
                    .apply()
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "Error fetching districts", e)
        }
    }

    protected suspend fun fetchRanksFromFirestore() {
        try {
            val snapshot = firestore.collection("rankMaster").whereEqualTo("isActive", true).get().await()
            val rankDocs = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("rank_id") ?: doc.id
                val order = doc.getLong("seniority_order")?.toInt() ?: 999
                val requiresMetal = doc.getBoolean("requiresMetalNumber") ?: false
                Triple(id, order, requiresMetal)
            }
            
            val sortedRankIds = rankDocs.sortedWith(
                compareBy<Triple<String, Int, Boolean>> { it.second }
                .thenBy { Constants.allRanksList.indexOf(it.first).let { if (it == -1) 9999 else it } }
            ).map { it.first }

            val metalRankIds = rankDocs.filter { it.third }.map { it.first }.sorted()

            if (sortedRankIds.isNotEmpty()) {
                prefs.edit()
                    .putString(RANKS_CACHE_KEY, gson.toJson(sortedRankIds))
                    .putString(RANKS_METAL_CACHE_KEY, gson.toJson(metalRankIds))
                    .apply()
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "Error fetching ranks", e)
        }
    }

    protected suspend fun fetchStationsFromFirestore() {
        try {
            val snapshot = firestore.collection("stations").get().await()
            val stationMap = mutableMapOf<String, MutableList<String>>()
            snapshot.documents.forEach { doc ->
                val name = doc.getString("name")
                val district = doc.getString("district")
                if (!name.isNullOrBlank() && !district.isNullOrBlank()) {
                    stationMap.getOrPut(district) { mutableListOf() }.add(name)
                }
            }
            if (stationMap.isNotEmpty()) {
                prefs.edit().putString(STATIONS_CACHE_KEY, gson.toJson(stationMap)).apply()
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "Error fetching stations", e)
        }
    }

    protected suspend fun fetchGlobalConfig() {
        try {
            val doc = firestore.collection("app_config").document("main_app").get().await()
            if (doc.exists()) {
                val hiddenFields = (doc.get("hiddenFields") as? List<String>) ?: emptyList()
                _globalHiddenFields.value = hiddenFields
                prefs.edit().putString(GLOBAL_CONFIG_CACHE_KEY, gson.toJson(hiddenFields)).apply()
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "Error fetching global config", e)
        }
    }

    protected fun loadGlobalConfigFromCache() {
        val json = prefs.getString(GLOBAL_CONFIG_CACHE_KEY, null)
        if (json != null) {
            val type = object : TypeToken<List<String>>() {}.type
            _globalHiddenFields.value = gson.fromJson(json, type)
        }
    }

    fun getRanks(): List<String> {
        val json = prefs.getString(RANKS_CACHE_KEY, null)
        if (json.isNullOrEmpty()) return Constants.allRanksList
        val ranks: List<String> = gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
        return ranks.sortedBy { rank ->
            val index = Constants.allRanksList.indexOf(rank)
            if (index == -1) 9999 else index
        }
    }

    fun getDistricts(): List<String> {
        val json = prefs.getString(DISTRICTS_CACHE_KEY, null)
        val baseList = if (json.isNullOrEmpty()) Constants.districtsList else gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
        return sortDistricts(baseList)
    }

    fun getDistrictShortCodes(): Map<String, String> {
        val json = prefs.getString(DISTRICT_SHORT_CODES_CACHE_KEY, null) ?: return emptyMap()
        return try {
            gson.fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getSubSections(): List<String> {
        val json = prefs.getString(SUB_SECTIONS_CACHE_KEY, null) ?: return DEFAULT_SUB_SECTIONS
        return try {
            val list: List<String> = gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
            if (list.isEmpty()) DEFAULT_SUB_SECTIONS else list
        } catch (e: Exception) {
            DEFAULT_SUB_SECTIONS
        }
    }

    protected suspend fun fetchSubSectionsFromFirestore() {
        try {
            val doc = firestore.collection("app_config").document("station_sub_sections").get().await()
            if (doc.exists()) {
                val list = (doc.get("items") as? List<*>)?.mapNotNull { it?.toString() } ?: DEFAULT_SUB_SECTIONS
                prefs.edit().putString(SUB_SECTIONS_CACHE_KEY, gson.toJson(list)).apply()
            } else {
                // Seed with defaults if document doesn't exist yet
                prefs.edit().putString(SUB_SECTIONS_CACHE_KEY, gson.toJson(DEFAULT_SUB_SECTIONS)).apply()
            }
        } catch (e: Exception) {
            Log.e("BaseConstantsRepo", "Error fetching sub-sections", e)
        }
    }

    fun getDutyRoleMapping(): Map<String, List<String>> {
        val json = prefs.getString(DUTY_ROLE_MAPPING_CACHE_KEY, null) ?: return emptyMap()
        return try {
            gson.fromJson(json, object : TypeToken<Map<String, List<String>>>() {}.type)
        } catch (e: Exception) {
            emptyMap()
        }
    }


    /**
     * Get duty roles for a specific unit based on mapping.
     * @param unitName The name of the unit.
     * @param onlyMapped If true, returns empty list if no specific roles are mapped (no global fallback).
     */
    fun getDutyRolesForUnit(unitName: String, onlyMapped: Boolean = false): List<String> {
        val unit = getFullUnits().find { it.name == unitName }
        val rolesForUnit = unit?.dutyRoles ?: emptyList()
        
        // If no specific roles are mapped for this unit
        if (rolesForUnit.isEmpty()) {
            if (onlyMapped) return listOf("Others")
            return (getSubSections() + "Others").distinct()
        }
        
        return if (onlyMapped) rolesForUnit else (rolesForUnit + "Others").distinct()
    }

    protected fun sortDistricts(districts: List<String>): List<String> {
        return districts.sortedWith(compareBy({ it != "HQ" }, { it }))
    }

    fun getUnits(): List<String> {
        val fullJson = prefs.getString(FULL_UNITS_CACHE_KEY, null)
        if (!fullJson.isNullOrEmpty()) {
            val full: List<UnitModel> = gson.fromJson(fullJson, object : TypeToken<List<UnitModel>>() {}.type)
            return full.map { it.name }
        }
        val json = prefs.getString(UNITS_CACHE_KEY, null)
        if (json.isNullOrEmpty()) return Constants.defaultUnitsList
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    fun getFullUnits(): List<UnitModel> {
        val json = prefs.getString(FULL_UNITS_CACHE_KEY, null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<UnitModel>>() {}.type)
    }

    fun getStationsByDistrict(): Map<String, List<String>> {
        val mergedMap = Constants.stationsByDistrictMap.toMutableMap()
        val json = prefs.getString(STATIONS_CACHE_KEY, null)
        if (!json.isNullOrEmpty()) {
            val firestoreMap: Map<String, List<String>> = gson.fromJson(json, object : TypeToken<Map<String, List<String>>>() {}.type)
            firestoreMap.forEach { (k, v) -> if (v.isNotEmpty()) mergedMap[k] = v }
        }
        return mergedMap
    }

    fun getBloodGroups(): List<String> = Constants.bloodGroupsList

    fun getRanksRequiringMetalNumber(): List<String> {
        val json = prefs.getString(RANKS_METAL_CACHE_KEY, null)
        if (json.isNullOrEmpty()) return Constants.ranksRequiringMetalNumber
        return gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }

    fun getDistrictsForUnit(unitName: String): List<String> {
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null) ?: return getDistricts() + "HQ"
        return try {
            val mappings: Map<String, UnitMapping> = gson.fromJson(json, object : TypeToken<Map<String, UnitMapping>>() {}.type)
            val mapping = mappings[unitName] ?: return sortDistricts(getDistricts() + "HQ")
            
            val hasAreaScope = mapping.scopes.any { it in listOf("district", "battalion", "commissionerate", "district_stations") }
            
            var districts = if (mapping.isHqLevel && !hasAreaScope) {
                listOf("HQ")
            } else {
                when (mapping.mappingType) {
                    "subset", "single" -> mapping.mappedDistricts
                    "none" -> listOf("No District Required")
                    else -> getDistricts()
                }
            }
            if (mapping.scopes.contains("state") || mapping.scopes.contains("hq") || mapping.isHqLevel) {
                districts = (districts + "HQ").distinct()
            }
            sortDistricts(districts)
        } catch (e: Exception) {
            sortDistricts(getDistricts() + "HQ")
        }
    }

    fun isDistrictLevelUnit(unitName: String): Boolean {
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null) ?: return false
        val mappings: Map<String, UnitMapping> = gson.fromJson(json, object : TypeToken<Map<String, UnitMapping>>() {}.type)
        return mappings[unitName]?.isDistrictLevel == true
    }

    suspend fun getSectionsForUnit(unitName: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("unit_sections").document(unitName).get().await()
            if (doc.exists()) {
                val sections = doc.get("sections") as? List<String>
                sections?.filter { it.isNotBlank() }?.sorted() ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getApplicableRanksForUnit(unitName: String): List<String> {
        val json = prefs.getString(UNIT_MAPPINGS_CACHE_KEY, null) ?: return getRanks()
        val mappings: Map<String, UnitMapping> = gson.fromJson(json, object : TypeToken<Map<String, UnitMapping>>() {}.type)
        val applicable = mappings[unitName]?.applicableRanks ?: emptyList()
        return if (applicable.isEmpty()) getRanks() else applicable.sortedBy { rank ->
            val index = Constants.allRanksList.indexOf(rank)
            if (index == -1) 9999 else index
        }
    }

    /**
     * Get the cache age in days
     */
    fun getCacheAgeDays(): Long {
        val timestamp = prefs.getLong(CACHE_TIMESTAMP_KEY, 0)
        if (timestamp == 0L) return Long.MAX_VALUE
        return (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
    }

    /**
     * Get stations AND sections combined for a unit+district combo
     * Used by CommonEmployeeForm to build the dropdown
     */
    suspend fun getStationsAndSectionsForUnit(unitName: String, district: String): List<String> = withContext(Dispatchers.IO) {
        val districtStations = getStationsByDistrict()[district] ?: emptyList()
        val filtered = getStationsForUnit(unitName, districtStations)
        val sections = getSectionsForUnit(unitName)
        (filtered + sections).distinct()
    }

    /**
     * Filter stations for a given unit. Override if unit-specific logic is needed.
     */
    open fun getStationsForUnit(unitName: String, districtStations: List<String>): List<String> = districtStations

    companion object {
        val DEFAULT_SUB_SECTIONS = listOf(
            "Writer", "Court", "Summons", "Crime", "IO", 
            "Night Duty", "Beat", "Patrol", "Guard", 
            "Reserve", "Driver", "Others"
        )
    }
}
