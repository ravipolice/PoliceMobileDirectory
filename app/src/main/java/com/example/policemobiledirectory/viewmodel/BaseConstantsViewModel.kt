package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.repository.BaseConstantsRepository
import com.example.policemobiledirectory.utils.Constants
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * BaseConstantsViewModel - Abstract class for managing constants in the UI.
 * Shared between user and admin flavors.
 */
abstract class BaseConstantsViewModel(
    protected val repository: BaseConstantsRepository
) : ViewModel() {

    private val _ranks = MutableStateFlow<List<String>>(repository.getRanks())
    val ranks: StateFlow<List<String>> = _ranks.asStateFlow()

    private val _units = MutableStateFlow<List<String>>(repository.getUnits())
    val units: StateFlow<List<String>> = _units.asStateFlow()

    private val _districts = MutableStateFlow<List<String>>(repository.getDistricts())
    val districts: StateFlow<List<String>> = _districts.asStateFlow()

    private val _districtShortCodeMap = MutableStateFlow<Map<String, String>>(repository.getDistrictShortCodes())
    val districtShortCodeMap: StateFlow<Map<String, String>> = _districtShortCodeMap.asStateFlow()

    private val _bloodGroups = MutableStateFlow<List<String>>(repository.getBloodGroups())
    val bloodGroups: StateFlow<List<String>> = _bloodGroups.asStateFlow()

    private val _stationsByDistrict = MutableStateFlow<Map<String, List<String>>>(repository.getStationsByDistrict())
    val stationsByDistrict: StateFlow<Map<String, List<String>>> = _stationsByDistrict.asStateFlow()

    private val _ranksRequiringMetalNumber = MutableStateFlow<List<String>>(repository.getRanksRequiringMetalNumber())
    val ranksRequiringMetalNumber: StateFlow<List<String>> = _ranksRequiringMetalNumber.asStateFlow()

    private val _fullUnits = MutableStateFlow(repository.getFullUnits())
    val fullUnits: StateFlow<List<com.example.policemobiledirectory.model.UnitModel>> = _fullUnits.asStateFlow()

    private val _subSectionList = MutableStateFlow<List<String>>(repository.getSubSections())
    val subSectionList: StateFlow<List<String>> = _subSectionList.asStateFlow()

    private val _dutyRoleMapping = MutableStateFlow<Map<String, List<String>>>(repository.getDutyRoleMapping())
    val dutyRoleMapping: StateFlow<Map<String, List<String>>> = _dutyRoleMapping.asStateFlow()

    // Static rank category lists (from Constants) exposed as StateFlows for UI consumption
    val ministerialRanks: StateFlow<Set<String>> = MutableStateFlow(Constants.ministerialRanks).asStateFlow()
    val policeStationRanks: StateFlow<Set<String>> = MutableStateFlow(Constants.policeStationRanks).asStateFlow()
    val highRankingOfficers: StateFlow<Set<String>> = MutableStateFlow(Constants.highRankingOfficers).asStateFlow()
    val ranksWithAutoAgid: StateFlow<Set<String>> = MutableStateFlow(Constants.ranksWithAutoAgid).asStateFlow()
    val ksrpBattalions: StateFlow<List<String>> = MutableStateFlow(Constants.ksrpBattalions).asStateFlow()

    // Global hidden fields from Firestore config
    val globalHiddenFields: StateFlow<List<String>> = repository.globalHiddenFields

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    init {
        // Always refresh units on init so admin rank/scope changes are immediate
        viewModelScope.launch {
            repository.refreshUnitsOnly()
            updateLocalState()
        }
        // Full refresh only if cache is stale (>1hr)
        if (repository.shouldRefreshCache()) {
            refreshConstants()
        }
    }

    /**
     * Refresh all constants from Firestore
     */
    fun refreshConstants() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncError.value = null
            val success = repository.refreshConstants()
            if (success) {
                updateLocalState()
            } else {
                _syncError.value = "Failed to sync constants from server"
            }
            _isLoading.value = false
        }
    }

    /** Alias for refreshConstants() used in some screens */
    fun forceRefresh() = refreshConstants()

    /**
     * Update StateFlows with latest data from repository
     */
    protected fun updateLocalState() {
        _ranks.value = repository.getRanks()
        _units.value = repository.getUnits()
        _districts.value = repository.getDistricts()
        _districtShortCodeMap.value = repository.getDistrictShortCodes()
        _bloodGroups.value = repository.getBloodGroups()
        _stationsByDistrict.value = repository.getStationsByDistrict()
        _ranksRequiringMetalNumber.value = repository.getRanksRequiringMetalNumber()
        _fullUnits.value = repository.getFullUnits()
        _subSectionList.value = repository.getSubSections()
        _dutyRoleMapping.value = repository.getDutyRoleMapping()
    }

    /**
     * Utility to filter districts based on selected unit
     */
    fun filterDistrictsForUnit(unitName: String): List<String> {
        return repository.getDistrictsForUnit(unitName)
    }

    /**
     * Returns district list for a given unit (alias used by form)
     */
    fun getDistrictsForUnit(unitName: String): List<String> = repository.getDistrictsForUnit(unitName)

    /**
     * Utility to get stations for a district
     */
    fun getStationsForDistrict(district: String): List<String> {
        return repository.getStationsByDistrict()[district] ?: emptyList()
    }

    /**
     * Get combined stations and sections for a unit+district combo
     */
    suspend fun getStationsAndSectionsForUnit(unitName: String, district: String): List<String> {
        return repository.getStationsAndSectionsForUnit(unitName, district)
    }

    /**
     * Get sections for a unit
     */
    suspend fun getSectionsForUnit(unitName: String): List<String> {
        return repository.getSectionsForUnit(unitName)
    }

    /**
     * Check if a rank requires a metal number
     */
    fun checkMetalNumberRequirement(rank: String): Boolean {
        return repository.getRanksRequiringMetalNumber().contains(rank)
    }

    /**
     * Check if a unit is district level
     */
    fun isDistrictLevelUnit(unitName: String): Boolean {
        return repository.isDistrictLevelUnit(unitName)
    }

    /**
     * Get applicable ranks for a unit
     */
    fun getApplicableRanksForUnit(unitName: String): List<String> {
        return repository.getApplicableRanksForUnit(unitName)
    }

    /**
     * Get duty roles for a specific unit based on mapping
     */
    fun getDutyRolesForUnit(unitName: String, onlyMapped: Boolean = false): List<String> {
        return repository.getDutyRolesForUnit(unitName, onlyMapped)
    }

    /**
     * Clear constants cache
     */
    fun clearCache() {
        repository.clearCache()
        updateLocalState()
    }
}
