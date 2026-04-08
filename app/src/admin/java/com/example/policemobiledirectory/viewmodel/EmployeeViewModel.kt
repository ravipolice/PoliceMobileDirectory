package com.example.policemobiledirectory.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.data.local.SessionManager
import com.example.policemobiledirectory.data.local.toEmployee
import com.example.policemobiledirectory.data.local.toEntity
import com.example.policemobiledirectory.repository.*
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.Officer
import com.example.policemobiledirectory.model.ExternalLinkInfo
import com.example.policemobiledirectory.ui.screens.GoogleSignInUiEvent
import com.example.policemobiledirectory.model.NotificationTarget
import com.example.policemobiledirectory.model.AppNotification
import com.example.policemobiledirectory.MainActivity
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.ui.theme.CardStyle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.PendingRegistrationRepository
import com.example.policemobiledirectory.repository.ConstantsRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.repository.ImageUploadRepository
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.repository.AppIconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.UUID


@HiltViewModel
open class EmployeeViewModel @Inject constructor(
    private val employeeRepo: EmployeeRepository,
    private val pendingRepo: PendingRegistrationRepository,
    val sessionManager: SessionManager,
    private val constantsRepository: ConstantsRepository,
    private val imageRepo: ImageRepository,
    private val syncRepository: SyncRepository,
    private val officerRepo: OfficerRepository,
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val aiSearchParser: com.example.policemobiledirectory.utils.AISearchParser
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val appIconRepository by lazy { AppIconRepository.create(context) }

    // (All your StateFlows are correctly defined here)
    private val _currentUser = MutableStateFlow<Employee?>(null)
    val currentUser: StateFlow<Employee?> = _currentUser.asStateFlow()
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    private val _authStatus = MutableStateFlow<OperationStatus<Employee>>(OperationStatus.Idle)
    val authStatus: StateFlow<OperationStatus<Employee>> = _authStatus.asStateFlow()
    private val _googleSignInUiEvent = MutableStateFlow<GoogleSignInUiEvent>(GoogleSignInUiEvent.Idle)
    val googleSignInUiEvent: StateFlow<GoogleSignInUiEvent> = _googleSignInUiEvent.asStateFlow()
    private val _isGoogleAccountPickerLoading = MutableStateFlow(false)
    val isGoogleAccountPickerLoading: StateFlow<Boolean> = _isGoogleAccountPickerLoading.asStateFlow()
    private val _otpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val otpUiState: StateFlow<OperationStatus<String>> = _otpUiState
    private val _verifyOtpUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val verifyOtpUiState: StateFlow<OperationStatus<String>> = _verifyOtpUiState
    private val _pinResetUiState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinResetUiState: StateFlow<OperationStatus<String>> = _pinResetUiState
    private val _pinChangeState = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pinChangeState: StateFlow<OperationStatus<String>> = _pinChangeState.asStateFlow()
    private var otpSentTime: Long? = null
    private val otpValidityDuration = 5 * 60 * 1000L
    private val _remainingTime = MutableStateFlow(0L)
    val remainingTime: StateFlow<Long> = _remainingTime
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    private val _employeeStatus = MutableStateFlow<OperationStatus<List<Employee>>>(OperationStatus.Loading)
    val employeeStatus: StateFlow<OperationStatus<List<Employee>>> = _employeeStatus.asStateFlow()

    private val _aiSearchStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val aiSearchStatus: StateFlow<OperationStatus<String>> = _aiSearchStatus.asStateFlow()
    
    // Officers (read-only contacts)
    private val _officers = MutableStateFlow<List<Officer>>(emptyList())
    val officers: StateFlow<List<Officer>> = _officers.asStateFlow()
    private val _officerStatus = MutableStateFlow<OperationStatus<List<Officer>>>(OperationStatus.Loading)
    val officerStatus: StateFlow<OperationStatus<List<Officer>>> = _officerStatus.asStateFlow()
    
    private val _officerPendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val officerPendingStatus: StateFlow<OperationStatus<String>> = _officerPendingStatus.asStateFlow()
    
    // --- Summary Statistics for Dashboard ---
    val employeesByDistrict: StateFlow<Map<String, Int>> = _employees.map { list ->
        list.groupingBy { it.district?.trim()?.ifEmpty { "N/A" } ?: "N/A" }.eachCount().toList()
            .sortedByDescending { it.second }.toMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val employeesByRank: StateFlow<Map<String, Int>> = _employees.map { list ->
        list.groupingBy { it.displayRank.trim().ifEmpty { "N/A" } }.eachCount().toList()
            .sortedByDescending { it.second }.toMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val officersByDistrict: StateFlow<Map<String, Int>> = _officers.map { list ->
        list.groupingBy { it.district?.trim()?.ifEmpty { "N/A" } ?: "N/A" }.eachCount().toList()
            .sortedByDescending { it.second }.toMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val officersByRank: StateFlow<Map<String, Int>> = _officers.map { list ->
        list.groupingBy { it.rank?.trim()?.ifEmpty { "N/A" } ?: "N/A" }.eachCount().toList()
            .sortedByDescending { it.second }.toMap()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    
    // Combined contacts (employees + officers) for unified search
    enum class StaffType { ALL, EMPLOYEE, OFFICER }

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
        val isHidden: Boolean get() = employee?.isHidden ?: officer?.isHidden ?: false
    }
    
    // --- Centralized Search Logic ---
    data class SearchParameters(
        val query: String = "",
        val filter: SearchFilter = SearchFilter.ALL,
        val district: String = "All",
        val station: String = "All",
        val rank: String = "All",
        val unit: String = "All", // New Unit filter
        val staffType: StaffType = StaffType.ALL,
        val showHidden: Boolean = false // Toggle for hidden contacts
    )
    
    // Unified Search Source of Truth
    private val _searchParams = MutableStateFlow(SearchParameters())
    val searchParams: StateFlow<SearchParameters> = _searchParams.asStateFlow()
    
    // Expose individual properties for UI convenience (backwards compatibility)
    val searchQuery: Flow<String> = _searchParams.map { it.query }
    val searchFilter: Flow<SearchFilter> = _searchParams.map { it.filter }
    val selectedDistrict: Flow<String> = _searchParams.map { it.district }
    val selectedStation: Flow<String> = _searchParams.map { it.station }
    val selectedRank: Flow<String> = _searchParams.map { it.rank }
    val selectedUnit: Flow<String> = _searchParams.map { it.unit }
    val selectedStaffType: Flow<StaffType> = _searchParams.map { it.staffType }

    // Update helpers
    fun updateSearchQuery(query: String) { _searchParams.value = _searchParams.value.copy(query = query) }
    fun updateSearchFilter(filter: SearchFilter) { _searchParams.value = _searchParams.value.copy(filter = filter) }
    fun updateSelectedDistrict(district: String) { 
        _searchParams.value = _searchParams.value.copy(
            district = district, 
            station = "All" // Reset station when district changes
        ) 
    }
    fun updateSelectedStation(station: String) { _searchParams.value = _searchParams.value.copy(station = station) }
    fun updateSelectedRank(rank: String) { _searchParams.value = _searchParams.value.copy(rank = rank) }
    fun updateSelectedUnit(unit: String) { 
        _searchParams.value = _searchParams.value.copy(
            unit = unit,
            station = if (unit != "All") "All" else _searchParams.value.station
        ) 
    }

    fun updateStaffType(staffType: StaffType) {
        _searchParams.value = _searchParams.value.copy(staffType = staffType)
    }

    fun updateShowHidden(show: Boolean) {
        _searchParams.value = _searchParams.value.copy(showHidden = show)
    }
    
    fun clearFilters() {
        _searchParams.value = SearchParameters()
    }

    fun performAISearch(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _aiSearchStatus.value = OperationStatus.Loading
            val structuredResult = aiSearchParser.parseSearchQuery(query)
            
            if (structuredResult != null) {
                // Apply AI-derived filters
                _searchParams.value = _searchParams.value.copy(
                    query = structuredResult.name ?: structuredResult.kgid ?: "",
                    rank = structuredResult.rank ?: "All",
                    district = structuredResult.district ?: "All",
                    station = structuredResult.station ?: "All",
                    unit = structuredResult.unit ?: "All"
                )
                _aiSearchStatus.value = OperationStatus.Success("AI Filters Applied")
                
                // Return to idle after a short feedback period
                delay(2000)
                _aiSearchStatus.value = OperationStatus.Idle
            } else {
                _aiSearchStatus.value = OperationStatus.Error("AI could not understand search")
                delay(2000)
                _aiSearchStatus.value = OperationStatus.Idle
            }
        }
    }

    // State for stations map loaded from repository
    private val _stationsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    init {
        // 1️⃣ Load stations map locally
        viewModelScope.launch {
            _stationsMap.value = constantsRepository.getStationsByDistrict()
        }

        // 2️⃣ Observe login state & Admin status from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
            }
        }

        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdminValue ->
                _isAdmin.value = isAdminValue
                
                // ✅ CRITICAL: Sync UID if admin to enable Firestore rules
                if (isAdminValue) {
                    val email = auth.currentUser?.email
                    val uid = auth.currentUser?.uid
                    if (!email.isNullOrBlank() && !uid.isNullOrBlank()) {
                        Log.d("EmployeeViewModel", "🔄 Starting Admin UID sync for $email")
                        employeeRepo.syncAdminUid(email, uid, null)
                        Log.d("EmployeeViewModel", "✅ Admin UID sync completed")
                    }
                    // Trigger sync once confirmed
                    Log.d("EmployeeViewModel", "🔄 Triggering initial pending refresh")
                    refreshPendingRegistrations()
                }
            }
        }

        // 3️⃣ Observe user email and fetch profile
        viewModelScope.launch {
            sessionManager.userEmail.collect { email ->
                if (email.isNotBlank()) {
                    // Try local first
                    val user = employeeRepo.getEmployeeByEmail(email)?.toEmployee()
                    if (user != null) {
                        _currentUser.value = user
                        // If user object says they are admin, upgrade state
                        if (user.isAdmin && !_isAdmin.value) {
                             _isAdmin.value = true
                        }
                    } else {
                        // Fallback to remote
                        val remoteUser = employeeRepo.getUserByEmail(email)
                        if (remoteUser is RepoResult.Success) {
                            _currentUser.value = remoteUser.data
                        }
                    }
                }
            }
        }
    }

    // Derived State: Stations for the selected district
    val stationsForSelectedDistrict: StateFlow<List<String>> = combine(_searchParams, _stationsMap) { params, stationsMap ->
        val district = params.district
        val selectedUnit = params.unit
        
        val baseStations = if (district == "All") {
             listOf("All") 
        } else {
            val stations = stationsMap[district] ?: run {
                val matchedKey = stationsMap.keys.firstOrNull { it.equals(district, ignoreCase = true) }
                if (matchedKey != null) stationsMap[matchedKey] else null
            } ?: emptyList()
            listOf("All") + stations
        }
        
        if (selectedUnit == "All") {
            baseStations
        } else {
            // Priority: Fetch unitSections from Firestore if it's a section-based unit
            // However, for search filters, we usually want to combine both or use repository logic
            val resolved = constantsRepository.getStationsForUnit(selectedUnit, baseStations.filter { it != "All" })
            listOf("All") + resolved
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allContacts: StateFlow<List<Contact>> = combine(_employees, _officers, _isAdmin) { employees, officers, isAdmin ->
        val filteredEmployees = if (isAdmin) employees else employees.filter { it.isApproved }
        val employeeContacts = filteredEmployees.map { Contact(employee = it) }
        val officerContacts = officers.map { Contact(officer = it) }
        employeeContacts + officerContacts
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _debouncedSearchQuery = _searchParams.map { it.query }
        .debounce(300)
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    // Optimized filteredContacts with simpler chaining
    val filteredContacts: StateFlow<List<Contact>> = combine(
        allContacts,
        _searchParams
    ) { contacts, params ->
        if (contacts.isEmpty()) return@combine emptyList<Contact>()

        val query = params.query.trim()
        val isGlobalSearch = query.isNotBlank()

        // 1. Initial Filtering by Hidden Status
        val visibleContacts = contacts.filter { it.isHidden == params.showHidden }

        // 2. Filter by Type (Employee/Officer)
        val filteredByType = when (params.staffType) {
            StaffType.ALL -> visibleContacts
            StaffType.EMPLOYEE -> visibleContacts.filter { it.employee != null }
            StaffType.OFFICER -> visibleContacts.filter { it.officer != null }
        }

        val filteredByDropdowns = if (isGlobalSearch) {
            filteredByType
        } else {
            filteredByType
                .filterByDistrict(params.district)
                .filterByStation(params.station)
                .filterByRank(params.rank)
                .filterByUnit(params.unit)
        }

        // 3. Sorting and Filtering Process
        if (query.isNotBlank()) {
            val queryLower = query.lowercase()
            filteredByDropdowns.filterByQuery(query, params.filter)
                .sortedByDescending { contact ->
                    when {
                        contact.employee != null -> contact.employee.matches(queryLower, "name") // Simple relevance check
                        contact.officer != null -> contact.officer.matches(queryLower, "name")
                        else -> false
                    }
                }
        } else {
            filteredByDropdowns.sortedBy { it.name }
        }
    }
    .flowOn(Dispatchers.Default)
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private fun normalizeDistrict(district: String?): String {
        if (district == null) return ""
        // Remove suffixes like " -NR", " -ER", " -BR", " -SR", " -WR", " -NER", " -CR", " -COP"
        return district.split(" -")[0].trim().lowercase()
    }

    // --- Helper Extension Functions for Filtering ---
    private fun List<Contact>.filterByDistrict(district: String): List<Contact> {
        if (district == "All") return this
        return this.filter { normalizeDistrict(it.district) == normalizeDistrict(district) }
    }

    private fun List<Contact>.filterByStation(station: String): List<Contact> {
        if (station == "All") return this
        return this.filter { it.station?.equals(station, ignoreCase = true) == true }
    }

    private fun List<Contact>.filterByRank(rank: String): List<Contact> {
        if (rank == "All") return this
        return this.filter { it.rank?.equals(rank, ignoreCase = true) == true }
    }
    
    private fun List<Contact>.filterByUnit(unit: String): List<Contact> {
        if (unit == "All") return this
        return this.filter { contact ->
            when {
                contact.employee != null -> unit.equals(contact.employee.effectiveUnit, ignoreCase = true)
                contact.officer != null -> unit.equals(contact.officer.effectiveUnit, ignoreCase = true)
                else -> false
            }
        }
    }

    private fun List<Contact>.filterByQuery(query: String, filterType: SearchFilter): List<Contact> {
        if (query.isBlank()) return this
        val queryLower = query.lowercase().trim()
        
        return this.filter { contact ->
            when {
                contact.employee != null -> {
                    val filterString = when (filterType) {
                        SearchFilter.NAME -> "name"
                        SearchFilter.KGID -> "kgid"
                        SearchFilter.MOBILE -> "mobile"
                        SearchFilter.STATION -> "station"
                        SearchFilter.RANK -> "rank"
                        SearchFilter.METAL_NUMBER -> "metal"
                        SearchFilter.BLOOD_GROUP -> "blood"
                        SearchFilter.ALL -> "all"
                    }
                    contact.employee.matches(queryLower, filterString)
                }
                contact.officer != null -> {
                     // Officers don't have metal number, but they DO have blood group
                     if (filterType == SearchFilter.METAL_NUMBER) false
                     else {
                         val filterString = when (filterType) {
                            SearchFilter.NAME -> "name"
                            SearchFilter.KGID -> "agid"
                            SearchFilter.MOBILE -> "mobile"
                            SearchFilter.STATION -> "station"
                             SearchFilter.RANK -> "rank"
                             SearchFilter.METAL_NUMBER -> "metal" // Won't reach here due to check above
                             SearchFilter.BLOOD_GROUP -> "blood"
                             SearchFilter.ALL -> "all"
                         }
                        contact.officer.matches(queryLower, filterString)
                     }
                }
                else -> false
            }
        }
    }

    private val _adminNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _adminNotificationsLastSeen = MutableStateFlow(0L)
    val adminNotificationsLastSeen = _adminNotificationsLastSeen.asStateFlow()

    // Filter admin notifications: Show only those newer than lastSeen
    val adminNotifications = combine(_adminNotifications, _adminNotificationsLastSeen) { notifications, lastSeen ->
        notifications.filter { (it.timestamp ?: 0L) > lastSeen }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _userNotifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _userNotificationsLastSeen = MutableStateFlow(0L)
    val userNotificationsLastSeen = _userNotificationsLastSeen.asStateFlow()

    // Filter user notifications: Show only those newer than lastSeen
    val userNotifications: StateFlow<List<AppNotification>> = combine(_userNotifications, _userNotificationsLastSeen) { notifications, lastSeen ->
        notifications.filter { (it.timestamp ?: 0L) > lastSeen }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Simplified filteredEmployees (reusing logic implicitly or explicitly if needed, but keeping separate for now as it returns Employee objects)
    val filteredEmployees: StateFlow<List<Employee>> = combine(_employees, _searchParams, _isAdmin) { employees, params, isAdmin ->
         if (employees.isEmpty()) return@combine emptyList<Employee>()
         val approvedEmployees = if (isAdmin) employees else employees.filter { it.isApproved }
         
         val query = params.query.trim()
         
         approvedEmployees
            .filter { params.district == "All" || normalizeDistrict(it.district) == normalizeDistrict(params.district) }
            .filter { params.station == "All" || it.station.equals(params.station, ignoreCase = true) }
            .filter { params.rank == "All" || it.rank.equals(params.rank, ignoreCase = true) }
            .filter { params.unit == "All" || it.effectiveUnit.equals(params.unit, ignoreCase = true) }
            .filter { 
                if (query.isBlank()) {
                    true 
                } else {
                    val filterString = when (params.filter) {
                        SearchFilter.NAME -> "name"
                        SearchFilter.KGID -> "kgid"
                        SearchFilter.MOBILE -> "mobile"
                        SearchFilter.STATION -> "station"
                        SearchFilter.RANK -> "rank"
                        SearchFilter.METAL_NUMBER -> "metal"
                        SearchFilter.BLOOD_GROUP -> "blood"
                        SearchFilter.ALL -> "all"
                    }
                    it.matches(query.lowercase().trim(), filterString)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val uploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()
    
    // Trigger for refreshing both pending sources
    private val _pendingRefreshTrigger = MutableStateFlow(0)
    
    // ✅ Source of truth combines two sources for full parity with Web Dashboard:
    // 1. Dedicated 'pending_registrations' collection (via pendingRepo)
    // 2. 'employees' collection where rank is "Pending Verification" (via employeeRepo)
    val pendingRegistrations: StateFlow<List<PendingRegistrationEntity>> = combine(
        _pendingRefreshTrigger,
        pendingRepo.getLocalPending(),
        employeeRepo.getPendingRegistrations()
    ) { trigger, fromPending, fromEmployees ->
        // Combine and deduplicate case-insensitively by KGID/Email
        val combined = (fromPending + fromEmployees)
            .distinctBy { it.kgid.trim().lowercase().ifBlank { it.email.trim().lowercase() } }
            .sortedByDescending { it.submittedAt?.time ?: it.createdAt?.time ?: 0L }
        Log.d("EmployeeViewModel", "📊 PendingRegistrations Combined: total=${combined.size}, fromPending=${fromPending.size}, fromEmployees=${fromEmployees.size}, trigger=$trigger")
        combined
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _pendingStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val pendingStatus: StateFlow<OperationStatus<String>> = _pendingStatus.asStateFlow()
    
    // Count of pending approvals for notification badge
    val pendingApprovalsTotalCount: StateFlow<Int> = pendingRegistrations.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Count of unviewed pending approvals (for Badge)
    // Count of unviewed pending approvals (for Badge)
    val unviewedPendingCount: StateFlow<Int> = pendingRegistrations.map { list ->
        list.count { !it.viewedByAdmin }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun markPendingRegistrationsAsViewed() {
        viewModelScope.launch {
            val unviewed = pendingRegistrations.value.filter { !it.viewedByAdmin }
            if (unviewed.isNotEmpty()) {
                unviewed.forEach { entity ->
                    pendingRepo.markAsViewed(entity)
                }
                // Refresh to ensure UI updates
                refreshPendingRegistrations()
            }
        }
    }

    private val _usefulLinks = MutableStateFlow<List<ExternalLinkInfo>>(emptyList())
    val usefulLinks: StateFlow<List<ExternalLinkInfo>> = _usefulLinks.asStateFlow()
    private val _operationResult = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val operationResult: StateFlow<OperationStatus<String>> = _operationResult.asStateFlow()
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()
    private val _firestoreToSheetStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val firestoreToSheetStatus: StateFlow<OperationStatus<String>> = _firestoreToSheetStatus.asStateFlow()

    private val _sheetToFirestoreStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val sheetToFirestoreStatus: StateFlow<OperationStatus<String>> = _sheetToFirestoreStatus.asStateFlow()

    private var userNotificationsListener: ListenerRegistration? = null
    private var userNotificationsListenerKgid: String? = null
    private var adminNotificationsListener: ListenerRegistration? = null

    // --- DELETED HARDCODED CONSTANTS ---
    // These caused stale data issues. Use ConstantsViewModel or inject ConstantsRepository instead.
    // val districts: StateFlow<List<String>> = MutableStateFlow(Constants.districtsList).asStateFlow()
    // val ranks: StateFlow<List<String>> = MutableStateFlow(Constants.allRanksList).asStateFlow()
    // val bloodGroups: StateFlow<List<String>> = MutableStateFlow(Constants.bloodGroupsList).asStateFlow()
    // val stationsByDistrict: StateFlow<Map<String, List<String>>> = MutableStateFlow(Constants.stationsByDistrictMap).asStateFlow()

    init {
        Log.d("EmployeeVM", "🟢 ViewModel initialized")

        loadSession()
        // Constants.kt is the primary source - no automatic syncing



        // 2️⃣ Observe login state from DataStore
        viewModelScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                _isLoggedIn.value = loggedIn
                Log.d("Session", "🔄 isLoggedIn = $loggedIn")
            }
        }

        // 🆕 Observe admin status from DataStore
        viewModelScope.launch {
            sessionManager.isAdmin.collect { isAdmin ->
                _isAdmin.value = isAdmin
                Log.d("Session", "🔄 isAdmin (reactive) = $isAdmin")
                // If admin status just confirmed, refresh pending registrations
                if (isAdmin) {
                    refreshPendingRegistrations()
                }
            }
        }

        // 3️⃣ Ensure signed in if needed
        viewModelScope.launch {
            try {
                ensureSignedInIfNeeded()
            } catch (e: Exception) {
                Log.e("Startup", "Startup failed: ${e.message}", e)
            }
        }

        // 4️⃣ Startup data prefetch based on established session
        // Note: Actual pending registration fetch now happens inside loadSession() 
        // once admin status is confirmed via live check.

        viewModelScope.launch {
            currentUser.collectLatest { user ->
                updateAdminNotificationListener(user?.isAdmin == true)
                updateUserNotificationListener(user)
            }
        }

        viewModelScope.launch {
            sessionManager.userNotificationsSeenAt.collect { lastSeen ->
                _userNotificationsLastSeen.value = lastSeen
            }
        }

        viewModelScope.launch {
            sessionManager.adminNotificationsSeenAt.collect { lastSeen ->
                _adminNotificationsLastSeen.value = lastSeen
            }
        }

        // 🆕 Observe pending approvals for Badge & System Notification
        viewModelScope.launch {
            pendingApprovalsTotalCount.collectLatest { count ->
                if (_isAdmin.value) {
                    updateBadgeNotification(count)
                } else {
                    clearBadgeNotification()
                }
            }
        }
    }

    /**
     * 🆕 Updates a persistent system notification with the current pending count.
     * This also updates the app icon badge count on supported Android launchers.
     */
    private fun updateBadgeNotification(count: Int) {
        if (count == 0) {
            clearBadgeNotification()
            return
        }

        val channelId = "pending_approval_channel_id"
        val notificationId = 9991 // Unique fixed ID for the badge notification

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Pending Approvals",
                android.app.NotificationManager.IMPORTANCE_LOW // Use LOW to avoid intrusive sound every update
            ).apply {
                description = "Shows the count of users awaiting approval"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_action", "view_pending_approvals")
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_IMMUTABLE else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.example.policemobiledirectory.R.drawable.app_logo)
            .setContentTitle("Pending Approvals")
            .setContentText("$count user(s) awaiting your approval")
            .setNumber(count) // 🏆 This sets the badge number
            .setOngoing(true) // Sticky notification
            .setAutoCancel(false)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 🆕 Clears the badge notification.
     */
    fun clearBadgeNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(9991)
    }

    // =========================================================
    // OPTIONAL: Manual Constants Sync from Google Sheets
    // =========================================================
    /**
     * Manually sync constants from Google Sheets (optional, for backup/restore)
     * This is NOT called automatically - only when admin explicitly requests it
     * Use Constants.kt as the primary source for better performance
     */
    fun syncConstantsFromSheet() = viewModelScope.launch {
        try {
            val success = constantsRepository.refreshConstants()
            if (success) {
                Log.d("EmployeeVM", "✅ Constants synced from Google Sheet (backup)")
                // Note: This updates cache, but app still uses Constants.kt
                // To actually use Sheet data, you'd need to update Constants.kt file manually
            } else {
                Log.e("EmployeeVM", "⚠️ Constants sync from Sheet failed")
            }
        } catch (e: Exception) {
            Log.e("EmployeeVM", "❌ Error syncing constants from Sheet: ${e.message}", e)
        }
    }

    // =========================================================
    // AUTHENTICATION (LOGIN, GOOGLE SIGN-IN, LOGOUT)
    // =========================================================
    fun loginWithPin(email: String, pin: String) {
        viewModelScope.launch {
            employeeRepo.loginUser(email, pin).collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val user = result.data
                        if (user != null) {
                            // ✅ Instantly update UI before waiting for DataStore
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin
                            _isLoggedIn.value = true

                            // ✅ Save to DataStore for persistence
                            sessionManager.saveLogin(email, user.isAdmin)

                            // ✅ Fetch a fresh version from local DB (ensures latest info)
                            val refreshed = employeeRepo.getEmployeeDirect(email)
                            if (refreshed != null) {
                                _currentUser.value = refreshed
                                _isAdmin.value = refreshed.isAdmin
                            }

                            _authStatus.value = OperationStatus.Success(user)
                            Log.d("Login", "✅ Logged in as ${user.name}, Admin=${user.isAdmin}")
                        } else {
                            _authStatus.value = OperationStatus.Error("User not found")
                        }
                    }

                    is RepoResult.Error -> {
                        _authStatus.value = OperationStatus.Error(result.message ?: "Login failed")
                    }

                    is RepoResult.Loading -> {
                        _authStatus.value = OperationStatus.Loading
                    }
                }
            }
        }
    }

    fun setGoogleAccountPickerLoading(loading: Boolean) {
        _isGoogleAccountPickerLoading.value = loading
    }


    fun handleGoogleSignIn(email: String, googleIdToken: String) {
        viewModelScope.launch {
            _googleSignInUiEvent.value = GoogleSignInUiEvent.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                if (authResult.user != null) {
                    val existingUser = employeeRepo.getEmployeeByEmail(email)
                    if (existingUser != null) {
                        val user = existingUser.toEmployee()
                        sessionManager.saveLogin(user.email, user.isAdmin)
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.SignInSuccess(user)
                    } else {
                        _googleSignInUiEvent.value = GoogleSignInUiEvent.RegistrationRequired(email, authResult.user?.displayName)
                    }
                } else {
                    _googleSignInUiEvent.value = GoogleSignInUiEvent.Error("Sign-in failed: Firebase user is null.")
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "❌ Failed", e)
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Error(e.localizedMessage ?: "Unknown error")
                logout()
            }
        }
    }

    //show real-time admin alert

    private fun updateAdminNotificationListener(isAdmin: Boolean) {
        if (!isAdmin) {
            adminNotificationsListener?.remove()
            adminNotificationsListener = null
            _adminNotifications.value = emptyList()
            return
        }

        if (adminNotificationsListener != null) return

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        adminNotificationsListener = firestore.collection("admin_notifications")
            .whereGreaterThan("timestamp", thirtyDaysAgo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("AdminNotifications", "❌ Failed to fetch: ${e.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val notifications = docs.mapNotNull { doc ->
                    doc.data?.toAppNotification(doc.id)
                }
                _adminNotifications.value = notifications
            }
    }

    fun markNotificationsRead(isAdminUser: Boolean, notifications: List<AppNotification>) {
        val latestTimestamp = notifications.mapNotNull { it.timestamp }.maxOrNull()
            ?: System.currentTimeMillis()
        viewModelScope.launch {
            if (isAdminUser) {
                if (latestTimestamp > _adminNotificationsLastSeen.value) {
                    sessionManager.setAdminNotificationsSeen(latestTimestamp)
                }
            } else {
                if (latestTimestamp > _userNotificationsLastSeen.value) {
                    sessionManager.setUserNotificationsSeen(latestTimestamp)
                }
            }
        }
    }

    /** Deletes admin_notifications older than 30 days from Firestore */
    fun deleteOldAdminNotifications() = viewModelScope.launch {
        try {
            val cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val oldDocs = firestore.collection("admin_notifications")
                .whereLessThan("timestamp", cutoff)
                .get()
                .await()
            for (doc in oldDocs.documents) {
                doc.reference.delete().await()
            }
            Log.d("EmployeeVM", "🗑️ Cleared ${oldDocs.size()} old admin notifications (>30 days)")
        } catch (e: Exception) {
            Log.e("EmployeeVM", "❌ Failed to delete old notifications: ${e.message}")
        }
    }

    private fun updateUserNotificationListener(user: Employee?) {
        val kgid = user?.kgid

        if (user?.isAdmin == true) {
            userNotificationsListener?.remove()
            userNotificationsListener = null
            userNotificationsListenerKgid = null
            _userNotifications.value = emptyList()
            return
        }

        if (userNotificationsListenerKgid == kgid) return

        userNotificationsListener?.remove()
        userNotificationsListener = null
        userNotificationsListenerKgid = kgid

        if (user == null || kgid.isNullOrBlank()) {
            _userNotifications.value = emptyList()
            return
        }

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        userNotificationsListener = firestore.collection("notifications_queue")
            .whereGreaterThan("timestamp", thirtyDaysAgo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserNotifications", "❌ Failed to fetch: ${e.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                val notifications = docs.mapNotNull { doc ->
                    val notification = doc.data?.toAppNotification(doc.id) ?: return@mapNotNull null
                    if (shouldDeliverNotification(notification, user)) notification else null
                }
                _userNotifications.value = notifications
            }
    }

    private fun shouldDeliverNotification(notification: AppNotification, user: Employee): Boolean {
        fun matches(lhs: String?, rhs: String?): Boolean =
            lhs != null && rhs != null && lhs.equals(rhs, ignoreCase = true)

        return when (notification.targetType) {
            NotificationTarget.ALL -> true
            NotificationTarget.ALL -> true
            NotificationTarget.INDIVIDUAL -> matches(notification.targetKgid, user.kgid)
            NotificationTarget.DISTRICT -> matches(notification.targetDistrict, user.district)
            NotificationTarget.STATION -> matches(notification.targetDistrict, user.district) &&
                    matches(notification.targetStation, user.station)
            NotificationTarget.KSRP_BATTALION -> matches(notification.targetDistrict, user.district)
            NotificationTarget.ADMIN -> user.isAdmin
        }
    }

    private fun Map<String, Any>.toAppNotification(id: String): AppNotification? {
        val title = this["title"] as? String ?: "Notification"
        val body = this["body"] as? String ?: "You have a new message."
        val timestamp = (this["timestamp"] as? Number)?.toLong()
        val targetType = (this["targetType"] as? String)?.runCatching {
            NotificationTarget.valueOf(this.uppercase())
        }?.getOrNull() ?: NotificationTarget.ALL
        val targetKgid = this["targetKgid"] as? String
        val targetDistrict = this["targetDistrict"] as? String
        val targetStation = this["targetStation"] as? String

        return AppNotification(
            id = id,
            title = title,
            body = body,
            timestamp = timestamp,
            targetType = targetType,
            targetKgid = targetKgid,
            targetDistrict = targetDistrict,
            targetStation = targetStation
        )
    }

    fun logout(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d("Logout", "🚪 Starting logout...")

                // 1️⃣ Clear local session FIRST to prevent observers from triggering
                sessionManager.clearSession()
                
                // 2️⃣ Reset in-memory session IMMEDIATELY
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null

                // 5️⃣ Reset auth/UI state so login screen doesn't re-trigger stale events
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle

                // 3️⃣ Sign out of Firebase (including any anonymous sessions)
                FirebaseAuth.getInstance().signOut()
                auth.signOut()
                
                // 4️⃣ Clear repository data
                employeeRepo.logout()

                Log.d("Logout", "✅ Logout complete, no anonymous re-login")

                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }

            } catch (e: Exception) {
                Log.e("Logout", "❌ Logout failed: ${e.message}")
                // Even if there's an error, ensure state is cleared
                _isLoggedIn.value = false
                _isAdmin.value = false
                _currentUser.value = null
                _authStatus.value = OperationStatus.Idle
                _googleSignInUiEvent.value = GoogleSignInUiEvent.Idle
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun uploadGalleryImage(uri: Uri, context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val downloadUrl = com.example.policemobiledirectory.helper.FirebaseStorageHelper.uploadPhoto(uri)

                // Optionally save to Firestore
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val data = hashMapOf(
                    "imageUrl" to downloadUrl,
                    "uploadedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("gallery").add(data)

                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- Officer Management ---


    fun deleteOfficer(officerId: String) {
        viewModelScope.launch {
            _officerPendingStatus.value = OperationStatus.Loading
            officerRepo.deleteOfficer(officerId).collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        _officerPendingStatus.value = OperationStatus.Success("Officer deleted successfully")
                        refreshOfficers() // Auto-refresh list
                    }
                    is RepoResult.Error -> {
                        _officerPendingStatus.value = OperationStatus.Error(result.message ?: "Failed to delete officer")
                    }
                    else -> Unit
                }
            }
        }
    }
    
    fun resetOfficerPendingStatus() {
        _officerPendingStatus.value = OperationStatus.Idle
    }


    // =========================================================
// OTP / PIN FLOW  (Secure Version)
// =========================================================
    fun sendOtp(email: String) {
        viewModelScope.launch {
            Log.d("ForgotPinFlow", "🟢 sendOtp() for $email")
            _otpUiState.value = OperationStatus.Loading

            try {
                when (val result = employeeRepo.sendOtp(email)) {
                    is RepoResult.Success -> {
                        _otpUiState.value = OperationStatus.Success(result.data ?: "OTP sent to $email")
                        startOtpCountdown()
                    }

                    is RepoResult.Error -> {
                        _otpUiState.value = OperationStatus.Error(result.message ?: "Failed to send OTP")
                    }

                    else -> Unit
                }

            } catch (e: Exception) {
                _otpUiState.value = OperationStatus.Error("Unexpected error: ${e.localizedMessage}")
            }
        }
    }


    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _verifyOtpUiState.value = OperationStatus.Loading
            try {
                when (val result = employeeRepo.verifyLoginCode(email, code)) {
                    is RepoResult.Success -> _verifyOtpUiState.value = OperationStatus.Success("OTP verified successfully")
                    is RepoResult.Error -> _verifyOtpUiState.value = OperationStatus.Error(result.message ?: "Invalid OTP")
                    else -> Unit
                }
            } catch (e: Exception) {
                _verifyOtpUiState.value = OperationStatus.Error(e.message ?: "Error verifying OTP")
            }
        }
    }

    fun updatePinAfterOtp(email: String, newPin: String) {
        viewModelScope.launch {
            _pinResetUiState.value = OperationStatus.Loading
            try {
                val result = employeeRepo.updateUserPin(email, null, newPin, true)
                when (result) {
                    is RepoResult.Success -> _pinResetUiState.value = OperationStatus.Success("PIN reset successful")
                    is RepoResult.Error -> _pinResetUiState.value = OperationStatus.Error(result.message ?: "Failed to reset PIN")
                    else -> Unit
                }
            } catch (e: Exception) {
                _pinResetUiState.value = OperationStatus.Error(e.message ?: "Error updating PIN")
            }
        }
    }

    fun changePin(email: String, oldPin: String, newPin: String) {
        viewModelScope.launch {
            _pinChangeState.value = OperationStatus.Loading
            when (val result = employeeRepo.updateUserPin(email, oldPin, newPin, false)) {
                is RepoResult.Success -> _pinChangeState.value = OperationStatus.Success("PIN changed successfully")
                is RepoResult.Error -> _pinChangeState.value = OperationStatus.Error(result.message ?: "Failed to change PIN")
                else -> Unit
            }
        }
    }

    private fun startOtpCountdown() {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            otpSentTime = start
            while (System.currentTimeMillis() - start < otpValidityDuration) {
                _remainingTime.value = otpValidityDuration - (System.currentTimeMillis() - start)
                delay(1000)
            }
            _remainingTime.value = 0L
            resetForgotPinFlow()
        }
    }

    fun resetForgotPinFlow() {
        _otpUiState.value = OperationStatus.Idle
        _verifyOtpUiState.value = OperationStatus.Idle
        _pinResetUiState.value = OperationStatus.Idle
    }

    fun resetPinChangeState() {
        _pinChangeState.value = OperationStatus.Idle
    }

    fun setPinResetError(message: String) {
        _pinResetUiState.value = OperationStatus.Error(message)
    }

    /**
     * Loads the user session from SessionManager.
     * If a valid session exists, it fetches user details and refreshes data.
     * If not, it ensures the app is in a clean, logged-out state.
     */
    fun loadSession() {
        viewModelScope.launch {
            // First, get the logged-in status.
            val isLoggedIn = sessionManager.isLoggedIn.first()
            _isLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                // If logged in, get the email and admin status.
                val email = sessionManager.userEmail.first()
                val persistedAdmin = sessionManager.isAdmin.first()
                _isAdmin.value = persistedAdmin

                if (email.isNotBlank()) {
                    try {
                        // 1. Fetch the full user object from the repository.
                        val userEntity = employeeRepo.getEmployeeByEmail(email)
                        val user = userEntity?.toEmployee()

                        if (user != null) {
                            _currentUser.value = user
                            _isAdmin.value = user.isAdmin || persistedAdmin
                            Log.d("Session", "✅ Session restored for user: ${user.name}, admin=${_isAdmin.value}")
                            
                            // Refresh base data
                            refreshEmployees()
                            refreshOfficers()

                            // 🔴 2. Background live Firestore check & Admin Sync on startup
                            viewModelScope.launch {
                                // Live check for account approval
                                val liveApproved = employeeRepo.checkIsApprovedFromFirestore(email)
                                if (liveApproved == false) {
                                    Log.w("Session", "🔴 Account DISABLED in Firestore. Logging out.")
                                    logout()
                                    return@launch
                                }

                                // Live check and sync for Admin status
                                val liveAdmin = employeeRepo.isAdminInFirestore(email)
                                val finalAdminState = liveAdmin || user.isAdmin
                                
                                if (finalAdminState) {
                                    Log.d("Session", "🛡️ EmployeeViewModel: Admin validated. Syncing UID & refreshing approvals...")
                                    _isAdmin.value = true
                                    
                                    // satisfy Firestore security rules
                                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                                        employeeRepo.syncAdminUid(email, uid, null)
                                        Log.d("EmployeeViewModel", "✅ Admin UID synced")
                                    }
                                    
                                    // Now safe to refresh pending registrations
                                    refreshPendingRegistrations()
                                } else {
                                    if (persistedAdmin) {
                                        Log.w("Session", "🚫 Admin status revoked in Firestore for EmployeeViewModel.")
                                        _isAdmin.value = false
                                        sessionManager.saveLogin(email, false)
                                    }
                                }
                            }
                        } else {
                            Log.e("Session", "❌ Session exists for $email but user not found in DB. Forcing logout.")
                            logout()
                        }
                    } catch (e: Exception) {
                        Log.e("Session", "❌ Error during session restore: ${e.message}")
                        logout()
                    }
                } else {
                    logout()
                }
            } else {
                // Not logged in. Ensure all states are clean.
                _isAdmin.value = false
                _currentUser.value = null
                Log.d("Session", "ℹ️ Guest Mode (EmployeeVM)")
            }
        }
    }

    // Optimized matching function (query is already lowercase)
    private fun Employee.matchesOptimized(queryLower: String, filter: SearchFilter): Boolean {
        return when (filter) {
            SearchFilter.NAME -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower)
            }
            SearchFilter.KGID -> {
                val kgidLower = kgid.lowercase()
                kgidLower.startsWith(queryLower) || kgidLower.contains(queryLower)
            }
            SearchFilter.MOBILE -> {
                // Mobile numbers: direct contains check (no lowercase needed for numbers)
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
            SearchFilter.ALL -> {
                val nameLower = name.lowercase()
                nameLower.startsWith(queryLower) || nameLower.contains(queryLower) ||
                kgid.lowercase().contains(queryLower) ||
                mobile1?.contains(queryLower) == true || mobile2?.contains(queryLower) == true ||
                station?.lowercase()?.contains(queryLower) == true ||
                rank?.lowercase()?.contains(queryLower) == true ||
                metalNumber?.lowercase()?.contains(queryLower) == true ||
                bloodGroup?.lowercase()?.contains(queryLower) == true || 
                unit?.lowercase()?.contains(queryLower) == true ||
                effectiveUnit.lowercase().contains(queryLower)
            }
        }
    }
    
    // Legacy function for backward compatibility (kept for Officer.matches)
    private fun Employee.matches(query: String, filter: SearchFilter): Boolean {
        return matchesOptimized(query.lowercase().trim(), filter)
    }


    // =========================================================
    // EMPLOYEE CRUD + HELPERS
    // =========================================================
    fun refreshEmployees() = viewModelScope.launch {
        if (_employees.value.isEmpty()) {
            _employeeStatus.value = OperationStatus.Loading
        }
        try {
            employeeRepo.refreshEmployees()
            val result = employeeRepo.getEmployees()
                .filterNot { it is RepoResult.Loading }
                .firstOrNull()
            when (result) {
                is RepoResult.Success -> {
                    val list = result.data ?: emptyList()
                    _employees.value = list
                    _employeeStatus.value = OperationStatus.Success(list)
                    // ✅ Intelligent Rating: Increment event count on successful data load
                    if (list.isNotEmpty()) {
                        sessionManager.incrementSuccessfulEventsCount()
                    }
                }
                is RepoResult.Error -> _employeeStatus.value = OperationStatus.Error(result.message ?: "Failed to load employees")
                else -> _employeeStatus.value = OperationStatus.Error("Failed to load employees")
            }
        } catch (e: Exception) {
            _employeeStatus.value = OperationStatus.Error("Refresh failed: ${e.message}")
        }
    }
    
    /**
     * ✅ Refresh current user data from Firestore
     * Call this after updating profile to ensure UI shows latest data
     */
    fun refreshCurrentUser() = viewModelScope.launch {
        val currentKgid = _currentUser.value?.kgid
        
        if (currentKgid != null) {
            try {
                // First try local DB (faster)
                val localEntity = employeeRepo.getLocalEmployeeByKgid(currentKgid)
                if (localEntity != null) {
                    _currentUser.value = localEntity.toEmployee()
                    Log.d("EmployeeViewModel", "✅ Refreshed current user from local: ${localEntity.name}, metalNumber=${localEntity.metalNumber}")
                }
                
                // Then refresh from Firestore to get latest data
                val firestoreDoc = firestore.collection("employees").document(currentKgid).get().await()
                val firestoreEmp = firestoreDoc.toObject(Employee::class.java)
                if (firestoreEmp != null) {
                    val finalEmp = firestoreEmp.copy(kgid = currentKgid)
                    _currentUser.value = finalEmp
                    Log.d("EmployeeViewModel", "✅ Refreshed current user from Firestore: ${finalEmp.name}, metalNumber=${finalEmp.metalNumber}")
                    
                    // Update local cache using mapper
                    val entity = finalEmp.toEntity()
                    employeeRepo.insertEmployeeDirect(entity)
                }
            } catch (e: Exception) {
                Log.e("EmployeeViewModel", "❌ Exception refreshing current user: ${e.message}", e)
            }
        } else {
            // Fallback: refresh by email
            try {
                val currentEmail = sessionManager.userEmail.first()
                if (currentEmail.isNotBlank()) {
                    when (val result = employeeRepo.getUserByEmail(currentEmail)) {
                        is RepoResult.Success -> {
                            result.data?.let { user ->
                                _currentUser.value = user
                                Log.d("EmployeeViewModel", "✅ Refreshed current user by email: ${user.name}, metalNumber=${user.metalNumber}")
                            }
                        }
                        is RepoResult.Error -> {
                            Log.e("EmployeeViewModel", "❌ Failed to refresh current user by email: ${result.message}")
                        }
                        else -> Unit
                    }
                }
            } catch (e: Exception) {
                Log.e("EmployeeViewModel", "❌ Exception refreshing current user by email: ${e.message}", e)
            }
        }
    }
    
    fun refreshOfficers() = viewModelScope.launch {
        if (_officers.value.isEmpty()) {
            _officerStatus.value = OperationStatus.Loading
        }
        try {
            // First sync from Firebase to Room
            officerRepo.syncAllOfficers()
            
            // Then observe Room via Repo
            officerRepo.getOfficers().collect { result ->
                when (result) {
                    is RepoResult.Success -> {
                        val list = result.data?.sortedBy { it.name } ?: emptyList()
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

    fun addOrUpdateEmployee(emp: Employee) = viewModelScope.launch {
        employeeRepo.addOrUpdateEmployee(emp).collect { result ->
            if (result is RepoResult.Success) {
                sessionManager.incrementSuccessfulEventsCount() // ✅ Intelligent Rating
            }
            refreshEmployees() 
        }
    }

    fun deleteEmployee(kgid: String, photoUrl: String?) = viewModelScope.launch {
        Log.d("DeleteEmployee", "Deleting employee $kgid...")

        // 1️⃣ Delete from Google Sheet + Room
        employeeRepo.deleteEmployee(kgid).collect {
            refreshEmployees()
        }

        // 2️⃣ Delete Drive photo (if available)
        photoUrl?.let { url ->
            val fileId = url.substringAfter("id=").substringBefore("&")
            Log.d("DeleteEmployee", "Attempting to delete image ID: $fileId")

            imageRepo.deleteOfficerImage(fileId, kgid).collect { status ->
                when (status) {
                    is OperationStatus.Idle -> {
                        Log.d("DriveDelete", "Idle — no operation started yet.")
                    }

                    is OperationStatus.Loading -> {
                        Log.d("DriveDelete", "Deleting image from Google Drive...")
                    }

                    is OperationStatus.Success -> {
                        Log.d("DriveDelete", status.data ?: "✅ Image deleted from Drive successfully.")
                    }

                    is OperationStatus.Error -> {
                        Log.e("DriveDelete", "❌ Drive deletion failed: ${status.message}")
                    }
                }
            }
        }
    }


    // ✅ FIX: ALL FUNCTIONS ARE NOW CORRECTLY PLACED AT THE TOP LEVEL OF THE CLASS
    // =========================================================
    // UI + MISC
    // =========================================================
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    /**
     * ✅ Prevents unwanted Firebase guest auto-login
     */
    private suspend fun ensureSignedInIfNeeded() {
        // ✅ Only check if we have a valid session in DataStore
        val hasValidSession = sessionManager.isLoggedIn.first()
        if (!hasValidSession) {
            // No valid session, ensure Firebase is signed out
            val user = auth.currentUser
            if (user != null) {
                Log.w("AuthCheck", "⚠️ Firebase user exists but no valid session — signing out.")
                try {
                    auth.signOut()
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Log.e("AuthCheck", "❌ Failed to sign out: ${e.message}")
                }
            }
            return
        }
        
        val user = auth.currentUser
        if (user == null) {
            Log.d("AuthCheck", "🔒 No Firebase user — not signing in automatically.")
            return
        }

        if (user.isAnonymous || user.email.isNullOrBlank()) {
            Log.w("AuthCheck", "⚠️ Anonymous Firebase session detected — signing out.")
            try {
                auth.signOut()
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e("AuthCheck", "❌ Failed to sign out anonymous user: ${e.message}")
            }
        } else {
            Log.d("AuthCheck", "✅ Valid Firebase user: ${user.email}")
        }
    }




    private suspend fun isEmailApproved(email: String): Boolean {
        // Prefer repository-level check (fast), but fallback to Firestore if needed.
        val emp = employeeRepo.getEmployeeByEmail(email) // returns entity or null
        return emp?.isApproved == true || emp?.toEmployee()?.isAdmin == true // adjust fields as per your model
    }
    fun refreshPendingRegistrations() = viewModelScope.launch {
        try {
            _pendingStatus.value = OperationStatus.Loading
            android.util.Log.d("PendingReg", "🔄 Triggering comprehensive background refresh...")

            val result = pendingRepo.fetchPendingFromFirestore()
            
            when (result) {
                is RepoResult.Success -> {
                    val list = result.data ?: emptyList()
                    android.util.Log.d("PendingReg", "✅ Success! Saving ${list.size} to Room from pending_registrations.")
                    pendingRepo.saveAllToLocal(list)   // sync to Room
                }

                is RepoResult.Error -> {
                    val errorMsg = result.message ?: "Load failed"
                    android.util.Log.w("PendingReg", "⚠️ Refresh failed: $errorMsg")
                    
                    // Only set error status if it's not a permission issue
                    if (errorMsg.contains("Permission", ignoreCase = true) || 
                        errorMsg.contains("permission denied", ignoreCase = true)) {
                        
                        if (_isAdmin.value) {
                            Log.e("PendingReg", "❌ Admin access denied to Firestore. Sync handshake might be pending.")
                            _pendingStatus.value = OperationStatus.Error("Access Denied. Ensure your admin status is active.")
                        } else {
                            Log.d("PendingReg", "Permission denied loading pending registrations (expected for non-admins)")
                            _pendingStatus.value = OperationStatus.Idle
                        }
                    } else {
                        _pendingStatus.value = OperationStatus.Error(errorMsg)
                    }
                }

                else -> {
                    _pendingStatus.value = OperationStatus.Idle
                }
            }

            // ✅ Trigger the combined StateFlow to re-run the employeeRepo.getPendingRegistrations() fetch
            _pendingRefreshTrigger.value += 1
            _pendingStatus.value = OperationStatus.Idle

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Load failed"
            if (errorMsg.contains("Permission", ignoreCase = true) || 
                errorMsg.contains("permission denied", ignoreCase = true)) {
                Log.d("PendingReg", "Permission denied loading pending registrations (expected for non-admins)")
                _pendingStatus.value = OperationStatus.Idle
            } else {
                _pendingStatus.value = OperationStatus.Error(errorMsg)
            }
        }
    }

    fun approveRegistration(entity: PendingRegistrationEntity) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            when (val result = pendingRepo.approve(entity)) {
                is RepoResult.Success -> {
                    _pendingStatus.value = OperationStatus.Success("Approved successfully")
                    sessionManager.incrementSuccessfulEventsCount() // ✅ Intelligent Rating
                    refreshPendingRegistrations()
                }
                is RepoResult.Error -> {
                    _pendingStatus.value = OperationStatus.Error(result.message ?: "Approval failed")
                }
                else -> Unit
            }
        }
    }

    // =========================================================
//  NEW USER REGISTRATION (Pending Approval + Admin Notification)
// =========================================================
    fun registerNewUser(entity: PendingRegistrationEntity) {
        // Prevent duplicate submissions
        if (_pendingStatus.value is OperationStatus.Loading) return
        _pendingStatus.value = OperationStatus.Loading

        viewModelScope.launch {
            try {
                // 1️⃣ Check for duplicate registration directly in Firestore (more reliable than cached list)
                val hasDuplicate = try {
                    // Check by KGID
                    val kgidSnapshot = firestore.collection("pending_registrations")
                        .whereEqualTo("status", "pending")
                        .whereEqualTo("kgid", entity.kgid)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!kgidSnapshot.isEmpty) {
                        true // Duplicate found by KGID
                    } else {
                        // Also check by email
                        val emailSnapshot = firestore.collection("pending_registrations")
                            .whereEqualTo("status", "pending")
                            .whereEqualTo("email", entity.email)
                            .limit(1)
                            .get()
                            .await()
                        !emailSnapshot.isEmpty // true if duplicate found
                    }
                } catch (e: Exception) {
                    Log.w("RegisterUser", "Duplicate check failed, proceeding anyway: ${e.message}")
                    false // Allow registration if check fails
                }

                if (hasDuplicate) {
                    _pendingStatus.value = OperationStatus.Error(
                        "A registration for this KGID/Email already exists and is pending approval."
                    )
                    return@launch
                }

                // 2️⃣ Prepare safe PendingRegistration object
                val pending = entity.copy(
                    isApproved = false,
                    firebaseUid = entity.firebaseUid.takeIf { it.isNotBlank() } ?: "",
                    status = "pending",
                    rejectionReason = null,
                    photoUrlFromGoogle = null
                )

                // 3️⃣ Submit to Firestore + Room
                pendingRepo.addPendingRegistration(pending).collect { result ->
                    when (result) {
                        is RepoResult.Loading ->
                            _pendingStatus.value = OperationStatus.Loading

                        is RepoResult.Success -> {
                            _pendingStatus.value =
                                OperationStatus.Success("Registration submitted for admin approval.")

                            // Refresh UI (only if admin)
                            if (_isAdmin.value) {
                                refreshPendingRegistrations()
                            }

                            // 4️⃣ Notify admin (don't wait for completion, send in background)
                            viewModelScope.launch {
                                try {
                                    sendNotification(
                                        title = "New User Registration Pending",
                                        body = "New registration from ${entity.name} (${entity.email}) awaiting approval.",
                                        target = NotificationTarget.ADMIN,
                                        d = entity.district,
                                        s = entity.station
                                    )
                                } catch (e: Exception) {
                                    Log.e("RegisterUser", "Failed to send notification: ${e.message}")
                                    // Don't fail registration if notification fails
                                }
                            }
                        }

                        is RepoResult.Error -> {
                            _pendingStatus.value =
                                OperationStatus.Error(result.message ?: "Registration failed.")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("RegisterUser", "❌ Registration failed", e)
                _pendingStatus.value =
                    OperationStatus.Error(e.localizedMessage ?: "Unexpected error")
            }
        }
    }

    fun updatePendingRegistration(entity: PendingRegistrationEntity, newPhotoUri: Uri?) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading
            try {
                var updatedEntity = entity
                if (newPhotoUri != null) {
                    val photoUrl = pendingRepo.uploadPhoto(entity, newPhotoUri)
                    updatedEntity = updatedEntity.copy(photoUrl = photoUrl)
                }

                when (val result = pendingRepo.updatePendingRegistration(updatedEntity)) {
                    is RepoResult.Success -> {
                        _pendingStatus.value = OperationStatus.Success("Pending registration updated.")
                        refreshPendingRegistrations()
                    }
                    is RepoResult.Error -> {
                        _pendingStatus.value = OperationStatus.Error(result.message ?: "Update failed.")
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e("PendingUpdate", "❌ Update failed", e)
                _pendingStatus.value = OperationStatus.Error(e.localizedMessage ?: "Update failed.")
            }
        }
    }




    fun rejectRegistration(entity: PendingRegistrationEntity, reason: String) {
        viewModelScope.launch {
            _pendingStatus.value = OperationStatus.Loading

            when (val result = pendingRepo.reject(entity, reason)) {
                is RepoResult.Success -> {
                    _pendingStatus.value = OperationStatus.Success("Rejected")
                    refreshPendingRegistrations()
                }
                is RepoResult.Error -> {
                    _pendingStatus.value = OperationStatus.Error(result.message ?: "Rejection failed")
                }
                else -> Unit
            }
        }
    }

    fun resetPendingStatus() {
        _pendingStatus.value = OperationStatus.Idle
    }

    // =========================================================
    //  USEFUL LINKS & NOTIFICATIONS
    // =========================================================
    fun fetchUsefulLinks() {
        viewModelScope.launch {
            try {
                val collection = firestore.collection("useful_links")
                val snapshot = try {
                    collection.get(Source.SERVER).await()
                } catch (serverError: Exception) {
                    Log.w("UsefulLinks", "Server fetch failed (${serverError.message}), falling back to cache")
                    collection.get(Source.CACHE).await()
                }

                // Temporary list for immediate show (no icons yet)
                _usefulLinks.value = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null
                    link.copy(documentId = doc.id)
                }

                // Fetch icons in background
                // Always try to fetch icons if playStoreUrl exists, even if iconUrl already exists
                // This allows refreshing icons that might be stale or incorrect
                val updatedLinks = snapshot.documents.mapNotNull { doc ->
                    val link = doc.toObject(ExternalLinkInfo::class.java) ?: return@mapNotNull null

                    val icon = if (!link.playStoreUrl.isNullOrBlank()) {
                        try {
                            // 🔥 ALWAYS fetch icon using favicon API (will use cache if valid)
                            val fetched = appIconRepository.getOrFetchAppIcon(link.playStoreUrl)

                            if (!fetched.isNullOrBlank()) {
                                // Only update Firestore if icon changed or was missing
                                if (link.iconUrl != fetched) {
                                    try {
                                        collection.document(doc.id).update("iconUrl", fetched).await()
                                        Log.d("IconUpdate", "Updated icon for ${link.name}")
                                    } catch (e: Exception) {
                                        Log.w("IconUpdate", "Failed to save icon for ${link.name}: ${e.message}")
                                    }
                                }
                                fetched
                            } else {
                                // If fetch failed, use existing iconUrl if available
                                link.iconUrl
                            }

                        } catch (e: Exception) {
                            Log.e("IconFetch", "Error fetching icon for ${link.name}: ${e.message}")
                            // Fallback to existing iconUrl if fetch failed
                            link.iconUrl
                        }

                    } else {
                        // No playStoreUrl, use existing iconUrl
                        link.iconUrl
                    }

                    link.copy(
                        iconUrl = icon,
                        documentId = doc.id
                    )
                }

                // 🔥 Update UI ONCE with full data
                _usefulLinks.value = updatedLinks

            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch useful links: ${e.message}")
                _usefulLinks.value = emptyList()
            }
        }
    }


    fun deleteUsefulLink(documentId: String) = viewModelScope.launch {
        try {
            _pendingStatus.value = OperationStatus.Loading
            firestore.collection("useful_links").document(documentId).delete().await()
            
            // ✅ Remove from local state immediately
            _usefulLinks.value = _usefulLinks.value.filter { it.documentId != documentId }
            _pendingStatus.value = OperationStatus.Success("Link deleted successfully")
            
            Log.d("UsefulLinks", "✅ Deleted link: $documentId")
        } catch (e: Exception) {
            Log.e("UsefulLinks", "❌ Failed to delete link: ${e.message}", e)
            _pendingStatus.value = OperationStatus.Error("Failed to delete: ${e.message}")
        }
    }

    fun addUsefulLink(
        name: String,
        playStoreUrl: String,
        apkUrl: String,
        iconUrl: String,
        apkFileUri: Uri?,
        imageUri: Uri?
    ) = viewModelScope.launch {
        _pendingStatus.value = OperationStatus.Loading

        try {
            var finalIconUrl = iconUrl.trim().takeIf { it.isNotBlank() }
            var finalApkUrl = apkUrl.trim().takeIf { it.isNotBlank() }

            // Upload APK file if provided
            if (finalApkUrl.isNullOrBlank() && apkFileUri != null) {
                Log.d("UsefulLinks", "Uploading APK file: $apkFileUri")
                finalApkUrl = uploadUsefulLinkApk(apkFileUri, name)
                if (finalApkUrl == null) {
                    throw Exception("Failed to upload APK file. Please check your internet connection and try again.")
                }
                Log.d("UsefulLinks", "APK uploaded successfully: $finalApkUrl")
            }

            // Upload icon image if provided
            if (finalIconUrl.isNullOrBlank() && imageUri != null) {
                Log.d("UsefulLinks", "Uploading icon image: $imageUri")
                finalIconUrl = uploadUsefulLinkIcon(imageUri, name)
                if (finalIconUrl == null) {
                    Log.w("UsefulLinks", "Icon upload failed, continuing without icon")
                } else {
                    Log.d("UsefulLinks", "Icon uploaded successfully: $finalIconUrl")
                }
            }

            // Fetch icon from Play Store if no icon provided
            if (finalIconUrl.isNullOrBlank() && playStoreUrl.isNotBlank()) {
                try {
                    finalIconUrl = appIconRepository.getOrFetchAppIcon(playStoreUrl)
                    Log.d("UsefulLinks", "Fetched icon from Play Store: $finalIconUrl")
                } catch (e: Exception) {
                    Log.w("UsefulLinks", "Icon fetch fallback failed: ${e.message}")
                }
            }

            val data = mutableMapOf<String, Any>(
                "name" to name.trim(),
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )

            if (playStoreUrl.isNotBlank()) data["playStoreUrl"] = playStoreUrl.trim()
            finalApkUrl?.let { 
                data["apkUrl"] = it
                Log.d("UsefulLinks", "Saving link with APK URL: $it")
            }
            finalIconUrl?.let { data["iconUrl"] = it }

            // Two separate flows validated:
            // Flow 1: Play Store link (APK optional)
            // Flow 2: APK file/URL (Play Store link optional)
            if (!data.containsKey("playStoreUrl") && !data.containsKey("apkUrl")) {
                throw IllegalArgumentException("Provide either Play Store URL OR APK file/URL")
            }

            Log.d("UsefulLinks", "Saving to Firestore: $data")
            firestore.collection("useful_links").add(data).await()
            Log.d("UsefulLinks", "✅ Link saved successfully to Firestore")

            _pendingStatus.value = OperationStatus.Success("Link added")
            fetchUsefulLinks()
        } catch (e: Exception) {
            Log.e("UsefulLinks", "❌ Failed to add link: ${e.message}", e)
            _pendingStatus.value = OperationStatus.Error(e.message ?: "Failed to add link")
        }
    }

    private suspend fun uploadUsefulLinkApk(apkUri: Uri, entryName: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val safeName = entryName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .ifBlank { "link" }
            val fileName = "${safeName}_${System.currentTimeMillis()}_${UUID.randomUUID()}.apk"
            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("useful_links/apks/$fileName")

            storageRef.putFile(apkUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("UsefulLinks", "APK upload failed: ${e.message}", e)
            null
        }
    }

    private suspend fun uploadUsefulLinkIcon(imageUri: Uri, entryName: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val safeName = entryName.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .ifBlank { "link" }
            val fileName = "${safeName}_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            val storageRef = FirebaseStorage.getInstance()
                .reference
                .child("useful_links/icons/$fileName")

            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("UsefulLinks", "Icon upload failed: ${e.message}", e)
            null
        }
    }

    fun syncFirebaseToSheet() = viewModelScope.launch {
        _firestoreToSheetStatus.value = OperationStatus.Loading
        val result = syncRepository.syncFirestoreToSheet()
        _firestoreToSheetStatus.value = result.fold(
            onSuccess = { OperationStatus.Success(it) },
            onFailure = { OperationStatus.Error(it.message ?: "Sync failed") }
        )
    }

    fun syncSheetToFirebase() = viewModelScope.launch {
        _sheetToFirestoreStatus.value = OperationStatus.Loading
        val result = syncRepository.syncSheetToFirestore()
        _sheetToFirestoreStatus.value = result.fold(
            onSuccess = { 
                // Refresh employees after successful sync
                refreshEmployees()
                OperationStatus.Success(it) 
            },
            onFailure = { OperationStatus.Error(it.message ?: "Sync failed") }
        )
    }
    
    private val _officersSyncStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val officersSyncStatus: StateFlow<OperationStatus<String>> = _officersSyncStatus.asStateFlow()
    
    fun syncOfficersSheetToFirebase() = viewModelScope.launch {
        _officersSyncStatus.value = OperationStatus.Loading
        val result = syncRepository.syncOfficersSheetToFirestore()
        _officersSyncStatus.value = result.fold(
            onSuccess = { 
                refreshOfficers() // Refresh officers list after sync
                OperationStatus.Success(it) 
            },
            onFailure = { OperationStatus.Error(it.message ?: "Sync failed") }
        )
    }
    
    fun resetOfficersSyncStatus() {
        _officersSyncStatus.value = OperationStatus.Idle
    }

    fun resetFirestoreToSheetStatus() {
        _firestoreToSheetStatus.value = OperationStatus.Idle
    }

    fun resetSheetToFirestoreStatus() {
        _sheetToFirestoreStatus.value = OperationStatus.Idle
    }

    fun sendNotification(
        title: String,
        body: String,
        target: NotificationTarget,
        k: String? = null,
        d: String? = null,
        s: String? = null
    ) = viewModelScope.launch {
        try {
            _pendingStatus.value = OperationStatus.Loading
            
            val request = hashMapOf(
                "title" to title,
                "body" to body,
                "targetType" to target.name,
                "targetKgid" to k?.takeIf { it.isNotBlank() },
                "targetDistrict" to d?.takeIf { it != "All" },
                "targetStation" to s?.takeIf { it != "All" },
                "timestamp" to System.currentTimeMillis(),
                "requesterKgid" to (_currentUser.value?.kgid ?: "unknown")
            )

            // ✅ Separate collection for admin notifications
            val collectionName = if (target == NotificationTarget.ADMIN)
                "admin_notifications"
            else
                "notifications_queue"

            firestore.collection(collectionName)
                .add(request)
                .await()
            
            _pendingStatus.value = OperationStatus.Success("Notification sent successfully.")
        } catch (e: Exception) {
            Log.e("EmployeeViewModel", "Error sending notification", e)
            _pendingStatus.value = OperationStatus.Error("Failed: ${e.message ?: "Unknown error"}")
        }
    }


    // =========================================================
    //  FILE UPLOADS
    // =========================================================
    fun uploadPhoto(uri: Uri, kgid: String) = viewModelScope.launch {
        imageRepo.uploadOfficerImage(uri, kgid).collect { status ->
            _uploadStatus.value = status
        }
    }

    // =========================================================
    //  UI CONTROLS
    // =========================================================
    fun updateEmployeeStatus(kgid: String, isApproved: Boolean) {
        viewModelScope.launch {
            employeeRepo.updateEmployeeFields(kgid, mapOf("isApproved" to isApproved)).collect { result ->
                if (result is RepoResult.Success) {
                    refreshEmployees()
                }
            }
        }
    }

    fun updateEmployeeVisibility(id: String, isHidden: Boolean, isOfficer: Boolean = false) {
        viewModelScope.launch {
            if (isOfficer) {
                officerRepo.updateOfficerFields(id, mapOf("isHidden" to isHidden)).collect { result ->
                    if (result is RepoResult.Success) {
                        refreshOfficers()
                    }
                }
            } else {
                employeeRepo.updateEmployeeFields(id, mapOf("isHidden" to isHidden)).collect { result ->
                    if (result is RepoResult.Success) {
                        refreshEmployees()
                    }
                }
            }
        }
    }

    // =========================================================
    // =========================================================
    //  UI CONTROLS
    // =========================================================

    // Note: Search/Filter update methods are now centralized at the top using _searchParams
    // Legacy methods removed to prevent conflicts


    fun adjustFontScale(increase: Boolean) {
        val step = 0.1f
        val current = _fontScale.value
        _fontScale.value = when {
            increase -> (current + step).coerceAtMost(1.8f)
            else -> (current - step).coerceAtLeast(0.8f)
        }
    }
    
    fun setFontScale(scale: Float) {
        _fontScale.value = scale.coerceIn(0.8f, 1.8f)
    }

    // Card Style State
    private val _currentCardStyle = MutableStateFlow<CardStyle>(CardStyle.Vibrant)
    val currentCardStyle: StateFlow<CardStyle> = _currentCardStyle.asStateFlow()

    fun updateCardStyle(style: CardStyle) {
        _currentCardStyle.value = style
    }

    // =========================================================
    // ADMIN CHECK
    // =========================================================
    fun checkIfAdmin() {
        viewModelScope.launch {
            try {
                // ✅ Use current user's email to check admin status (more reliable than uid)
                val email = _currentUser.value?.email
                if (email.isNullOrBlank()) {
                    // Fallback to session email if currentUser is not set
                    val sessionEmail = sessionManager.userEmail.first()
                    if (sessionEmail.isNotBlank()) {
                        val user = employeeRepo.getEmployeeByEmail(sessionEmail)
                        _isAdmin.value = user?.isAdmin ?: false
                        Log.d("AdminCheck", "✅ Admin status from session: ${user?.isAdmin}")
                    } else {
                        _isAdmin.value = false
                    }
                    return@launch
                }
                
                // ✅ Check admin status from current user or refresh from repository
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    _isAdmin.value = currentUser.isAdmin
                    Log.d("AdminCheck", "✅ Admin status from currentUser: ${currentUser.isAdmin}")
                } else {
                    // Refresh from repository
                    val user = employeeRepo.getEmployeeByEmail(email)
                    _isAdmin.value = user?.isAdmin ?: false
                    Log.d("AdminCheck", "✅ Admin status from repository: ${user?.isAdmin}")
                }
            } catch (e: Exception) {
                Log.e("AdminCheck", "❌ Error checking admin status: ${e.message}")
            }
        }
    }

    private val _saveStatus = MutableStateFlow<RepoResult<Boolean>?>(null)
    val saveStatus: StateFlow<RepoResult<Boolean>?> = _saveStatus.asStateFlow()

    val photoUploadStatus: StateFlow<OperationStatus<String>> = _uploadStatus.asStateFlow()

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    fun saveEmployee(employee: Employee, photoUri: Uri?) {
        viewModelScope.launch {
            _saveStatus.value = RepoResult.Loading
            try {
                var finalEmployee = employee

                // 1. Upload photo if exists
                if (photoUri != null) {
                    _uploadStatus.value = OperationStatus.Loading
                    val uploadResult = imageRepo.uploadOfficerImage(photoUri, employee.kgid)
                        .onEach { status -> _uploadStatus.value = status }
                        .filter { it is OperationStatus.Success || it is OperationStatus.Error }
                        .first()
                    
                    if (uploadResult is OperationStatus.Error) {
                        _saveStatus.value = RepoResult.Error(Exception(uploadResult.message))
                        return@launch
                    } else if (uploadResult is OperationStatus.Success) {
                         finalEmployee = finalEmployee.copy(photoUrl = uploadResult.data)
                    }
                }
                
                // 2. Save Employee
                employeeRepo.addOrUpdateEmployee(finalEmployee).collect { result ->
                    _saveStatus.value = result
                    if (result is RepoResult.Success) {
                        refreshEmployees()
                        refreshCurrentUser()
                    }
                }

            } catch (e: Exception) {
                _saveStatus.value = RepoResult.Error(e)
            } finally {
                _uploadStatus.value = OperationStatus.Idle
            }
        }
    }

    // This generic helper can be used if needed, but isn't strictly necessary with the current implementations
    private fun <T> launchOperationForResult(stateFlow: MutableStateFlow<OperationStatus<T>>, block: suspend () -> Flow<RepoResult<T>>) = viewModelScope.launch {
        stateFlow.value = OperationStatus.Loading
        block().collectLatest { result ->
            when (result) {
                is RepoResult.Loading -> stateFlow.value = OperationStatus.Loading
                is RepoResult.Success -> stateFlow.value = OperationStatus.Success(result.data ?: return@collectLatest)
                is RepoResult.Error -> stateFlow.value = OperationStatus.Error(result.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userNotificationsListener?.remove()
        userNotificationsListener = null
        adminNotificationsListener?.remove()
        adminNotificationsListener = null
    }
}
