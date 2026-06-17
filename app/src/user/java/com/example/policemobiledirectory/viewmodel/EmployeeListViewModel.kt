package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.OfficerRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.PerformanceLogger
import com.example.policemobiledirectory.utils.SearchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * ViewModel responsible for employee and officer list operations:
 * - Employee CRUD operations
 * - Employee search and filtering
 * - Officer list management
 * - Combined contacts (employees + officers)
 */
@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val officerRepo: OfficerRepository,
    private val aiSearchParser: com.example.policemobiledirectory.utils.AISearchParser
) : ViewModel() {

    // Employee State
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _employeeStatus = MutableStateFlow<OperationStatus<List<Employee>>>(OperationStatus.Loading)
    val employeeStatus: StateFlow<OperationStatus<List<Employee>>> = _employeeStatus.asStateFlow()

    // Officers State (read-only contacts)
    private val _officers = MutableStateFlow<List<Officer>>(emptyList())
    val officers: StateFlow<List<Officer>> = _officers.asStateFlow()

    private val _officerStatus = MutableStateFlow<OperationStatus<List<Officer>>>(OperationStatus.Loading)
    val officerStatus: StateFlow<OperationStatus<List<Officer>>> = _officerStatus.asStateFlow()

    private val _aiSearchStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val aiSearchStatus: StateFlow<OperationStatus<String>> = _aiSearchStatus.asStateFlow()

    // ✅ Sync Throttling
    private var lastEmployeeSyncTime = 0L
    private var lastOfficerSyncTime = 0L
    private val SYNC_THROTTLE_MS = 5 * 60 * 1000L // 5 minutes

    // Combined contacts (employees + officers) for unified search
    data class Contact(
        val employee: Employee? = null,
        val officer: Officer? = null
    ) {
        val name: String get() = employee?.name ?: officer?.name ?: ""
        val id: String get() = employee?.kgid ?: officer?.agid ?: ""
        val rank: String? get() = employee?.rank ?: officer?.rank
        val station: String? get() = employee?.station ?: officer?.station
        val district: String? get() = employee?.district ?: officer?.district
        val mobile1: String? get() = employee?.mobile1 ?: officer?.primaryPhone
        val photoUrl: String? get() = employee?.photoUrl ?: employee?.photoUrlFromGoogle ?: officer?.photoUrl
    }

    // Search and Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _debouncedSearchQuery = MutableStateFlow("")
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _selectedUnit = MutableStateFlow("All")
    val selectedUnit: StateFlow<String> = _selectedUnit.asStateFlow()

    private val _selectedDistrict = MutableStateFlow("All")
    val selectedDistrict: StateFlow<String> = _selectedDistrict.asStateFlow()

    private val _selectedStation = MutableStateFlow("All")
    val selectedStation: StateFlow<String> = _selectedStation.asStateFlow()

    private val _selectedRank = MutableStateFlow("All")
    val selectedRank: StateFlow<String> = _selectedRank.asStateFlow()

    // Rank Priority Map (Highest Rank = Lowest Index)
    private val rankPriorityMap = mapOf(
        // Senior Officers
        "DYSP" to 1,
        // Inspectors
        "PI" to 2, "CPI" to 2, "RPI" to 2, "WPI" to 2, "PIW" to 2,
        // Sub-Inspectors
        "PSI" to 3, "WPSI" to 3, "RSI" to 3, "PSIW" to 3,
        // Asst Sub-Inspectors
        "ASI" to 4, "WASI" to 4, "ARSI" to 4, "ASIW" to 4,
        // Head Constables
        "HC" to 5, "AHC" to 5, "CHC" to 5, "WHC" to 5, "HCW" to 5,
        // Constables
        "PC" to 6, "APC" to 6, "CPC" to 6, "WPC" to 6, "PCW" to 6,
        // Ministerial / Staff
        "SS" to 7, "FDA" to 8, "SDA" to 9, "GHA" to 10, "AO" to 11,
        "Typist" to 12, "Steno" to 13, "PA" to 14
    )

    private fun getRankPriority(rank: String?): Int {
        if (rank.isNullOrBlank()) return 999
        val normalized = rank.trim().uppercase()
        return rankPriorityMap[normalized] ?: 998
    }

    private data class SearchFilters(
        val query: String,
        val filter: SearchFilter,
        val unit: String,
        val district: String,
        val station: String,
        val rank: String
    )

    private val filtersFlow1 = combine(_debouncedSearchQuery, _searchFilter, _selectedUnit) { q, f, u -> Triple(q, f, u) }
    private val filtersFlow2 = combine(_selectedDistrict, _selectedStation, _selectedRank) { d, s, r -> Triple(d, s, r) }

    private val searchFiltersFlow = combine(filtersFlow1, filtersFlow2) { f1, f2 ->
        SearchFilters(f1.first, f1.second, f1.third, f2.first, f2.second, f2.third)
    }

    // Admin state (needed for filtering approved employees)
    private val _isAdmin = MutableStateFlow(false)
    fun setIsAdmin(isAdmin: Boolean) {
        _isAdmin.value = isAdmin
    }

    // Unified Power Search (Room-based)
    private val _powerSearchResults = MutableStateFlow<List<Contact>>(emptyList())

    // Combined contacts with admin-aware filtering
    val allContacts: StateFlow<List<Contact>> = combine(_employees, _officers, _isAdmin) { employees, officers, isAdmin ->
        val filteredEmployees = if (isAdmin) employees else employees.filter { it.isApproved }
        
        val registeredKgids = filteredEmployees.map { it.kgid.trim().lowercase() }.toHashSet()
        val registeredEmails = filteredEmployees.mapNotNull { it.email?.trim()?.lowercase() }.filter { it.isNotBlank() }.toHashSet()
        
        val normalizeMobile = { m: String? ->
            if (m.isNullOrBlank()) null
            else {
                val digits = m.replace(Regex("\\D"), "")
                if (digits.length >= 10) digits.takeLast(10) else digits
            }
        }
        val registeredPhones = filteredEmployees.flatMap { 
            listOfNotNull(normalizeMobile(it.mobile1), normalizeMobile(it.mobile2))
        }.filter { it.isNotBlank() }.toHashSet()

        val employeeContacts = filteredEmployees.map { Contact(employee = it) }
        val officerContacts = officers.filter { off ->
            val offAgid = off.agid.trim().lowercase()
            val offEmail = off.email?.trim()?.lowercase() ?: ""
            val offMobile1 = normalizeMobile(off.mobile)
            val offMobile2 = normalizeMobile(off.mobile2)
            
            val agidMatch = offAgid.isNotBlank() && registeredKgids.contains(offAgid)
            val emailMatch = offEmail.isNotBlank() && registeredEmails.contains(offEmail)
            val phoneMatch = (offMobile1 != null && registeredPhones.contains(offMobile1)) || 
                             (offMobile2 != null && registeredPhones.contains(offMobile2))
                             
            !agidMatch && !emailMatch && !phoneMatch
        }.map { Contact(officer = it) }
        
        employeeContacts + officerContacts
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * ✅ Unified Power Search Strategy:
     * 1. If query is blank: Use allContacts + dropdown filters.
     * 2. If query present: Search Room (both tables), combine, then apply dropdown filters.
     */
    val filteredContacts: StateFlow<List<Contact>> = combine(
        allContacts,
        _powerSearchResults,
        searchFiltersFlow,
        _isAdmin
    ) { contacts, powerResults, filters, isAdmin ->
        val query = filters.query
        val unit = filters.unit
        val district = filters.district
        val station = filters.station
        val rank = filters.rank

        val sourceList = if (query.isNotBlank()) powerResults else contacts
        
        if (query.isNotBlank()) {
            val queryLower = query.trim().lowercase()
            sourceList.sortedByDescending { contact ->
                when {
                    contact.employee != null -> SearchEngine.calculateEmployeeScore(contact.employee, queryLower, filters.filter)
                    contact.officer != null -> SearchEngine.calculateOfficerScore(contact.officer, queryLower, filters.filter.name)
                    else -> 0.0
                }
            }
        } else {
            sourceList.sortedWith(
                compareBy<Contact> { getRankPriority(it.rank) }
                    .thenBy { it.name }
            )
        }.filter { contact ->
            // if Global Search (query present), ignore dropdown filters and search whole DB
            val isGlobalSearch = query.isNotBlank()

            val districtMatch = isGlobalSearch || district == "All" || contact.district?.equals(district, ignoreCase = true) == true
            
            val stationMatch = if (isGlobalSearch || station == "All") {
                true
            } else {
                val isPS = station.endsWith(" PS", ignoreCase = true)
                val stripped = if (isPS) station.dropLast(3).trim() else station
                val circleVariant = "$stripped Circle"
                val contactStation = contact.station?.trim()
                (contactStation?.equals(station, ignoreCase = true) == true) ||
                (isPS && (contactStation?.equals(circleVariant, ignoreCase = true) == true || 
                         contactStation?.equals(stripped, ignoreCase = true) == true)) ||
                ((contactStation.isNullOrBlank() || contactStation.equals("Others", ignoreCase = true)) && 
                 contact.name.contains(stripped, ignoreCase = true))
            }

            val rankMatch = isGlobalSearch || rank == "All" || contact.rank?.equals(rank, ignoreCase = true) == true
            
            val unitMatch = isGlobalSearch || unit == "All" || contact.employee?.unit?.equals(unit, ignoreCase = true) == true
            
            districtMatch && stationMatch && rankMatch && unitMatch
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Legacy support for filteredEmployees if still used elsewhere
    val filteredEmployees: StateFlow<List<Employee>> = filteredContacts.map { contacts ->
        contacts.mapNotNull { it.employee }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Power Search Logic
        viewModelScope.launch {
            _debouncedSearchQuery.collect { query ->
                if (query.isBlank()) {
                    _powerSearchResults.value = emptyList()
                    return@collect
                }
                
                // Unified Room Search
                combine(
                    employeeRepo.searchByBlob(query),
                    officerRepo.searchByBlob(query)
                ) { empResult, offResult ->
                    val emps = (empResult as? RepoResult.Success)?.data ?: emptyList()
                    val offs = (offResult as? RepoResult.Success)?.data ?: emptyList()
                    
                    val isAdmin = _isAdmin.value
                    val filteredEmps = if (isAdmin) emps else emps.filter { it.isApproved }
                    
                    val registeredKgids = filteredEmps.map { it.kgid.trim().lowercase() }.toHashSet()
                    val registeredEmails = filteredEmps.mapNotNull { it.email?.trim()?.lowercase() }.filter { it.isNotBlank() }.toHashSet()
                    
                    val normalizeMobile = { m: String? ->
                        if (m.isNullOrBlank()) null
                        else {
                            val digits = m.replace(Regex("\\D"), "")
                            if (digits.length >= 10) digits.takeLast(10) else digits
                        }
                    }
                    val registeredPhones = filteredEmps.flatMap { 
                        listOfNotNull(normalizeMobile(it.mobile1), normalizeMobile(it.mobile2))
                    }.filter { it.isNotBlank() }.toHashSet()

                    val employeeContacts = filteredEmps.map { Contact(employee = it) }
                    val officerContacts = offs.filter { off ->
                        val offAgid = off.agid.trim().lowercase()
                        val offEmail = off.email?.trim()?.lowercase() ?: ""
                        val offMobile1 = normalizeMobile(off.mobile)
                        val offMobile2 = normalizeMobile(off.mobile2)
                        
                        val agidMatch = offAgid.isNotBlank() && registeredKgids.contains(offAgid)
                        val emailMatch = offEmail.isNotBlank() && registeredEmails.contains(offEmail)
                        val phoneMatch = (offMobile1 != null && registeredPhones.contains(offMobile1)) || 
                                         (offMobile2 != null && registeredPhones.contains(offMobile2))
                                         
                        !agidMatch && !emailMatch && !phoneMatch
                    }.map { Contact(officer = it) }
                    
                    employeeContacts + officerContacts
                }.collect { combined ->
                    _powerSearchResults.value = combined
                }
            }
        }

        // Debounce search query (300ms) to avoid searching on every keystroke
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    _debouncedSearchQuery.value = query
                }
        }
        
        // Initial setup
        refreshEmployees()
        refreshOfficers()
    }

    // =========================================================
    // EMPLOYEE OPERATIONS
    // =========================================================

    fun refreshEmployees() = viewModelScope.launch {
        // ⏱️ Throttle full syncs
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmployeeSyncTime < SYNC_THROTTLE_MS && _employees.value.isNotEmpty()) {
            Log.d("ListVM", "⏱️ Skipping refreshEmployees (throttled)")
            return@launch
        }

        // 🔄 Silent Loading
        if (_employees.value.isEmpty()) {
            _employeeStatus.value = OperationStatus.Loading
        }

        try {
            PerformanceLogger.measureDatabaseOperation("employees", "refresh") {
                employeeRepo.refreshEmployees()
                lastEmployeeSyncTime = currentTime
                val result = employeeRepo.getEmployees()
                    .filterNot { it is RepoResult.Loading }
                    .firstOrNull()
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _employees.value = list
                        _employeeStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> _employeeStatus.value = OperationStatus.Error(result.message ?: "Failed to load employees")
                    else -> _employeeStatus.value = OperationStatus.Error("Failed to load employees")
                }
            }
        } catch (e: Exception) {
            _employeeStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect {
            refreshEmployees()
        }
    }

    fun deleteEmployee(kgid: String) = viewModelScope.launch {
        Log.d("DeleteEmployee", "Deleting employee $kgid...")
        employeeRepo.deleteEmployee(kgid).collect {
            refreshEmployees()
        }
    }

    // =========================================================
    // OFFICER OPERATIONS
    // =========================================================

    fun refreshOfficers() = viewModelScope.launch {
        // ⏱️ Throttle full syncs
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastOfficerSyncTime < SYNC_THROTTLE_MS && _officers.value.isNotEmpty()) {
            Log.d("ListVM", "⏱️ Skipping refreshOfficers (throttled)")
            return@launch
        }

        // 🔄 Silent Loading
        if (_officers.value.isEmpty()) {
            _officerStatus.value = OperationStatus.Loading
        }

        try {
            // First sync from Firebase to Room
            officerRepo.syncAllOfficers()
            lastOfficerSyncTime = currentTime
            
            // Then observe Room via Repo
            officerRepo.getOfficers().collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data ?: emptyList()
                        _officers.value = list
                        _officerStatus.value = OperationStatus.Success(list)
                    }
                    is RepoResult.Error -> {
                        _officerStatus.value = OperationStatus.Error(result.message ?: "Failed to load officers")
                    }
                    is RepoResult.Loading -> {
                        _officerStatus.value = OperationStatus.Loading
                    }
                }
            }
        } catch (e: Exception) {
            _officerStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }

    // =========================================================
    // SEARCH AND FILTER OPERATIONS
    // =========================================================

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    fun updateSelectedUnit(unit: String) {
        _selectedUnit.value = unit
    }

    fun updateSelectedDistrict(district: String) {
        _selectedDistrict.value = district
    }

    fun updateSelectedStation(station: String) {
        _selectedStation.value = station
    }

    fun updateSelectedRank(rank: String) {
        _selectedRank.value = rank
    }

    fun performAISearch(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _aiSearchStatus.value = OperationStatus.Loading
            val structuredResult = aiSearchParser.parseSearchQuery(query)
            
            if (structuredResult != null) {
                // Apply AI-derived filters
                _searchQuery.value = structuredResult.name ?: structuredResult.kgid ?: ""
                _selectedRank.value = structuredResult.rank ?: "All"
                _selectedDistrict.value = structuredResult.district ?: "All"
                _selectedStation.value = structuredResult.station ?: "All"
                _selectedUnit.value = structuredResult.unit ?: "All"
                
                _aiSearchStatus.value = OperationStatus.Success("AI Filters Applied")
                
                delay(2000)
                _aiSearchStatus.value = OperationStatus.Idle
            } else {
                _aiSearchStatus.value = OperationStatus.Error("AI could not understand search")
                delay(2000)
                _aiSearchStatus.value = OperationStatus.Idle
            }
        }
    }

    // =========================================================
    // HELPER FUNCTIONS
    // =========================================================

    // Optimized matching function (query is already lowercase)
    private fun Employee.matchesOptimized(queryLower: String, filter: SearchFilter): Boolean {
        return when (filter) {
            SearchFilter.ALL -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower) ||
                kgid.lowercase().contains(queryLower) ||
                mobile1?.contains(queryLower) == true || mobile2?.contains(queryLower) == true ||
                station?.lowercase()?.contains(queryLower) == true ||
                rank?.lowercase()?.contains(queryLower) == true ||
                metalNumber?.lowercase()?.contains(queryLower) == true ||
                bloodGroup?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.NAME -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            SearchFilter.KGID -> {
                val kgidLower = kgid.lowercase()
                kgidLower.startsWith(queryLower) || kgidLower.contains(queryLower)
            }
            SearchFilter.MOBILE -> {
                mobile1?.contains(queryLower) == true || mobile2?.contains(queryLower) == true
            }
            SearchFilter.STATION -> {
                station?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.RANK -> {
                rank?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.METAL_NUMBER -> {
                metalNumber?.lowercase()?.contains(queryLower) == true
            }
            SearchFilter.BLOOD_GROUP -> {
                bloodGroup?.lowercase()?.contains(queryLower) == true
            }
        }
    }

    /**
     * Instant lookup by ID directly from already-loaded in-memory lists.
     * Avoids waiting for allContacts StateFlow (which starts as emptyList) to emit.
     */
    fun findContactById(id: String, isOfficer: Boolean): Any? {
        return if (isOfficer) {
            _officers.value.firstOrNull { it.agid == id }
        } else {
            _employees.value.firstOrNull { it.kgid == id }
        }
    }
}



