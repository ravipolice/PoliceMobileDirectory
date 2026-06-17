package com.example.policemobiledirectory.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.policemobiledirectory.data.local.PendingRegistrationEntity
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * CommonEmployeeForm
 *
 * - isAdmin: admin add/edit
 * - isSelfEdit: user editing own profile
 * - isRegistration: registration mode (shows PIN/terms)
 *
 * Callbacks:
 * - onSubmit(employee, photoUri) for admin/self-edit
 * - onRegisterSubmit(pendingEntity, photoUri) for registration
 */

// Validators
fun isValidEmail(v: String) = v.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(v).matches()
fun isValidMobile(v: String) = v.filter { it.isDigit() }.length in 10..13
fun isKgidValid(v: String) = v.isNotBlank() && v.length >= 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonEmployeeForm(
    isAdmin: Boolean,
    isSelfEdit: Boolean,
    isRegistration: Boolean,
    initialEmployee: Employee? = null,
    initialKgid: String? = null,
    initialEmail: String = "", // ✅ Add initialEmail parameter for prefilling
    initialName: String = "",
    onSubmit: (Employee, Uri?) -> Unit,
    onRegisterSubmit: ((PendingRegistrationEntity, Uri?) -> Unit)? = null,
    isLoading: Boolean = false, // ✅ Add loading state parameter
    onNavigateToTerms: (() -> Unit)? = null, // ✅ Callback to navigate to terms
    constantsViewModel: ConstantsViewModel = hiltViewModel(),
    isOfficer: Boolean = false, // ✅ New parameter for Officer mode
    isEdit: Boolean = false, // ✅ New parameter to handle ID field editability
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) } // ✅ Track submission state
    var pinVisible by remember { mutableStateOf(false) }
    var confirmPinVisible by remember { mutableStateOf(false) }

    // Get constants from ViewModel
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val subSectionList by constantsViewModel.subSectionList.collectAsStateWithLifecycle()
    // val dutyRoleMapping by constantsViewModel.dutyRoleMapping.collectAsStateWithLifecycle() // No longer used
    val bloodGroups by constantsViewModel.bloodGroups.collectAsStateWithLifecycle()

    val ranksRequiringMetalNumber by constantsViewModel.ranksRequiringMetalNumber.collectAsStateWithLifecycle()

    val ministerialRanks by constantsViewModel.ministerialRanks.collectAsStateWithLifecycle()
    val policeStationRanks by constantsViewModel.policeStationRanks.collectAsStateWithLifecycle()
    val highRankingOfficers by constantsViewModel.highRankingOfficers.collectAsStateWithLifecycle()
    val units by constantsViewModel.units.collectAsStateWithLifecycle()
    val fullUnits by constantsViewModel.fullUnits.collectAsStateWithLifecycle() // ✅ Need full objects for hidden flag
    val globalHiddenFields by constantsViewModel.globalHiddenFields.collectAsStateWithLifecycle() // ✅ Global Hidden Fields
    val ksrpBattalions by constantsViewModel.ksrpBattalions.collectAsStateWithLifecycle()
    val unitSections by constantsViewModel.currentUnitSections.collectAsStateWithLifecycle() // ✅ Fix: Collect unitSections

    LaunchedEffect(Unit) {
        constantsViewModel.forceRefresh()
    }

    // Identify if it has custom sections (e.g. State INT or manual sections)
    // Need this early for visibility logic
    // (district is a var state defined below, but we can use remember here too)


    // Filter hidden units for Registration Form
    val filteredUnits = remember(units, fullUnits, isRegistration) {
        if (isRegistration) {
            val hiddenUnitNames = fullUnits.filter { it.hideFromRegistration }.map { it.name }.toSet()
            units.filter { !hiddenUnitNames.contains(it) }
        } else {
            units
        }
    }

    // fields
    var kgid by remember(initialEmployee, initialKgid) { mutableStateOf(initialEmployee?.kgid ?: initialKgid.orEmpty()) }
    var name by remember(initialEmployee) { mutableStateOf(initialEmployee?.name ?: "") }
    // ✅ Use initialEmail if provided, otherwise use initialEmployee.email
    var email by remember(initialEmployee, initialEmail) { 
        mutableStateOf(initialEmployee?.email ?: initialEmail) 
    }
    var mobile1 by remember(initialEmployee) { mutableStateOf(initialEmployee?.mobile1 ?: "") }
    var mobile2 by remember(initialEmployee) { mutableStateOf(initialEmployee?.mobile2 ?: "") }
    var landline by remember(initialEmployee) { mutableStateOf(initialEmployee?.landline ?: "") }
    var landline2 by remember(initialEmployee) { mutableStateOf(initialEmployee?.landline2 ?: "") }
    var rank by remember(initialEmployee) { mutableStateOf(initialEmployee?.rank ?: "") }
    var metalNumber by remember(initialEmployee) { mutableStateOf(initialEmployee?.metalNumber ?: "") }
    var district by remember(initialEmployee) { mutableStateOf(initialEmployee?.district ?: "") }
    var station by remember(initialEmployee) { mutableStateOf(if (initialEmployee?.isManualStation == true) "Others" else initialEmployee?.station ?: "") }
    var subSection by remember(initialEmployee) { mutableStateOf(if (initialEmployee?.isManualSubSection == true) "Others" else initialEmployee?.subSection ?: "") }
    var manualSubSection by remember(initialEmployee) { mutableStateOf(if (initialEmployee?.isManualSubSection == true) initialEmployee.subSection.orEmpty() else "") }
    var unit by remember(initialEmployee) { mutableStateOf(initialEmployee?.unit ?: "") }
    var bloodGroup by remember(initialEmployee) { mutableStateOf(initialEmployee?.bloodGroup ?: "") }
    var currentPhotoUrl by remember(initialEmployee) { mutableStateOf(initialEmployee?.photoUrl) }
    var croppedPhotoUri by remember(initialEmployee) { mutableStateOf<Uri?>(null) }

    // KCSR specific extras
    var gender by remember(initialEmployee) { mutableStateOf(initialEmployee?.gender ?: "Male") }
    var serviceStartDate by remember(initialEmployee) { mutableStateOf<Date?>(initialEmployee?.serviceStartDate) }
    var dateOfBirth by remember(initialEmployee) { mutableStateOf<Date?>(initialEmployee?.dateOfBirth) }
    var genderExpanded by remember { mutableStateOf(false) }

    // registration extras
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    
    // ✅ Get selected Unit Model to check for hidden fields (Unit Level)
    val selectedUnitModel = remember(unit, fullUnits) {
        fullUnits.find { it.name == unit }
    }

    val isSpecialUnit = remember(selectedUnitModel) {
        selectedUnitModel?.mappingType == "none"
    }

    // Helper to check if field is visible (Hybrid Logic: Global OR Unit)
    // Returns TRUE if field is NOT hidden in Global AND NOT hidden in Unit
    val isFieldVisible: (String) -> Boolean = { fieldId ->
        // Hide landline fields on registration form
        if (isRegistration && (fieldId == "landline" || fieldId == "landline2")) {
            false
        } else {
            val isHiddenGlobally = globalHiddenFields.contains(fieldId)
            val isHiddenInUnit = selectedUnitModel?.hiddenFields?.contains(fieldId) == true
            !isHiddenGlobally && !isHiddenInUnit
        }
    }

    // Dynamic Label Logic (Depends on Unit Configuration)
    val isDistrictLevelByModel by produceState(initialValue = false, key1 = unit) {
        value = constantsViewModel.isDistrictLevelUnit(unit)
    }

    val stationLabel = remember(unit, isDistrictLevelByModel, selectedUnitModel) {
        val keyword = selectedUnitModel?.stationKeyword
        if (!keyword.isNullOrBlank()) {
            keyword
        } else if (unit == "Law & Order" || !isDistrictLevelByModel) {
            "Station"
        } else {
            "Section / Branch"
        }
    }

    val identifierLabel = remember(isOfficer, selectedUnitModel) {
        val base = if (isOfficer) "ID" else "KGID"
        val filter = selectedUnitModel?.identifierFilter
        if (!filter.isNullOrBlank()) "$base ($filter)" else "$base*"
    }


    // UI states
    var rankExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
    var subSectionExpanded by remember { mutableStateOf(false) }
    var manualSection by remember(initialEmployee) { mutableStateOf(if (initialEmployee?.isManualStation == true) initialEmployee.station.orEmpty() else "") }
    var bloodGroupExpanded by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showValidationErrors by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val showMetalNumberField = remember(rank, ranksRequiringMetalNumber) { ranksRequiringMetalNumber.contains(rank) }
    // Check if rank is ministerial (does not require station)
    val isMinisterial = remember(rank, ministerialRanks) {
        ministerialRanks.any { it.equals(rank, ignoreCase = true) }
    }
    
    // Check if rank is High Ranking Officer (No District/Station, uses AGID)
    val isHighRankingOfficer = remember(rank, highRankingOfficers) {
        highRankingOfficers.contains(rank)
    }

    // Check if rank triggers Auto-generate AGID and hide field
    val isAutoAgidRank = remember(rank) {
        Constants.ranksWithAutoAgid.contains(rank)
    }

    val isAutoAgid = isAutoAgidRank
    val isDistrictLevelUnit = isDistrictLevelByModel
    val hasSections = remember(unitSections, unit, district) { unitSections.isNotEmpty() || unit == "State INT" || district == "HQ" }

    LaunchedEffect(unit) {
        constantsViewModel.loadSectionsForUnit(unit)
    }

    // Auto-generate AGID for high-ranking officers if it's empty
    LaunchedEffect(isAutoAgidRank, rank) {
        if (isAutoAgidRank && kgid.isBlank()) {
            val randomId = (10000000..99999999).random()
            kgid = "OFF_$randomId"
        }
    }

    // Dynamic District List Logic
    // Dynamic District List Logic (Hybrid Strategy)
    val availableDistricts by produceState(initialValue = emptyList(), key1 = unit, key2 = districts) {
        value = constantsViewModel.getDistrictsForUnit(unit)
    }

    // Dynamic Rank List Logic (Filters based on Unit if configured)
    val applicableRanks by produceState<List<String>>(initialValue = emptyList(), key1 = unit) {
        if (unit.isNotBlank()) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                constantsViewModel.getApplicableRanksForUnit(unit)
            }
        } else {
            value = emptyList()
        }
    }

    val filteredRanks = remember(ranks, applicableRanks) {
        if (applicableRanks.isNotEmpty()) {
            ranks.filter { rankName -> 
                applicableRanks.any { it.equals(rankName, ignoreCase = true) }
            }
        } else {
            ranks
        }
    }

    // Reset district/station if Unit changes (but respect dynamic configuration)
    // Validate District Selection when available options change
    LaunchedEffect(availableDistricts) {
        if (availableDistricts.isNotEmpty()) {
            if (availableDistricts.size == 1) {
                 val autoSelected = availableDistricts.first()
                 if (district != autoSelected) {
                     district = autoSelected
                 }
            } else {
                 if (district.isNotBlank() && !availableDistricts.contains(district)) {
                     district = ""
                     station = ""
                 }
            }
        }
    }

    // Reset station when district changes (only if it's an actual change from previous)
    var previousDistrict by remember(initialEmployee) { mutableStateOf(district) }
    LaunchedEffect(district) {
        if (district != previousDistrict) {
            if (district.isNotBlank() && station.isNotBlank()) {
                 station = ""
            }
            previousDistrict = district
        }
    }
    
    // Validate Station Selection (e.g. Unit change might invalidate station even if district is same)


    // Reset rank if unit changes and the currently selected rank is no longer applicable
    LaunchedEffect(filteredRanks) {
        if (rank.isNotBlank() && filteredRanks.isNotEmpty() && !filteredRanks.contains(rank)) {
            rank = ""
        }
    }

    // Reset manual section if station selection changes away from "Others"
    LaunchedEffect(station) {
        if (station != "Others") {
            manualSection = ""
        }
    }

    // Reset manual sub-section if duty role selection changes away from "Others"
    LaunchedEffect(subSection) {
        if (subSection != "Others") {
            manualSubSection = ""
        }
    }

    // Reset sub-section when station changes
    LaunchedEffect(station) {
        if (station.isNotBlank()) {
            // subSection = "" // Maybe don't clear it, as same roles exist across stations
        }
    }

    // 🔹 CENTRALIZED STATIONS & SECTIONS LOGIC
    val stationsForSelectedDistrict by produceState<List<String>>(initialValue = emptyList(), key1 = unit, key2 = district) {
        value = constantsViewModel.getStationsAndSectionsForUnit(unit, district)
    }

    // Identify if label should be "Station" or "Section"
    // DEPRECATED in favor of dynamic stationLabel above
    val useStationLabel = remember(unit, isDistrictLevelByModel) {
        unit == "Law & Order" || !isDistrictLevelByModel
    }

    // Refactored for readability as per review
    // Standardized resolution via ViewModel
    val stationsForSelectedDistrictActual = stationsForSelectedDistrict // Placeholder for better naming if needed
    

    // Validate Station Selection when options change (e.g. Unit change filters stations)
    LaunchedEffect(stationsForSelectedDistrict) {
         if (stationsForSelectedDistrict.isNotEmpty() && station.isNotBlank() && station != "Others" && !stationsForSelectedDistrict.contains(station)) {
                station = ""
         }
    }

    // Temp URI for camera capture
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // UCrop launcher
    val uCropResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            UCrop.getOutput(result.data!!)?.let { croppedPhotoUri = it }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val err = UCrop.getError(result.data!!)
            Toast.makeText(context, "Crop error: ${err?.message}", Toast.LENGTH_SHORT).show()
        } else {
            Unit
        }
    }

    // Camera launcher (TakePicture)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            launchUCrop(context, tempCameraUri!!, uCropResultLauncher)
        }
    }

    // Prepare camera capture
    fun launchCamera() {
        val uri = createTempImageUri(context)
        if (uri != null) {
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Failed to create temp file", Toast.LENGTH_SHORT).show()
        }
    }

    // camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    // gallery
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { launchUCrop(context, it, uCropResultLauncher) }
    }

    // validators moved to top level

    val fieldSpacing = 6.dp
    val sectionSpacing = 10.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Upgraded Profile Photo selector area (premium circular design)
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showSourceDialog = true }
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        croppedPhotoUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(croppedPhotoUri),
                                contentDescription = "Selected Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        !currentPhotoUrl.isNullOrBlank() -> {
                            Image(
                                painter = rememberAsyncImagePainter(currentPhotoUrl),
                                contentDescription = "Existing Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Select Photo",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Add Photo",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { showSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (croppedPhotoUri != null || !currentPhotoUrl.isNullOrBlank()) Icons.Default.Edit else Icons.Default.AddAPhoto,
                        contentDescription = "Edit Photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isRegistration) {
            // ==================== REGISTRATION FORM ====================
            
            // 1. Personal Info Card
            FormSectionCard(
                title = "Personal Information",
                icon = Icons.Default.Person
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name*") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showValidationErrors && name.isBlank()
                )
                if (showValidationErrors && name.isBlank()) {
                    Text("Name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
                
                Spacer(Modifier.height(12.dp))

                // Gender Selection (Full Width)
                if (isFieldVisible("gender")) {
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender*") },
                            leadingIcon = { Icon(Icons.Default.Face, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                            listOf("Male", "Female").forEach { selection ->
                                DropdownMenuItem(text = { Text(selection) }, onClick = {
                                    gender = selection
                                    genderExpanded = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Row for Date of Birth & Blood Group
                val showDob = isFieldVisible("dob")
                val showBlood = !isOfficer && isFieldVisible("bloodGroup")
                if (showDob || showBlood) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showDob) {
                            val dobCalendar = Calendar.getInstance()
                            dateOfBirth?.let { dobCalendar.time = it }
                            val dobDatePicker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    dateOfBirth = cal.time
                                },
                                dobCalendar.get(Calendar.YEAR),
                                dobCalendar.get(Calendar.MONTH),
                                dobCalendar.get(Calendar.DAY_OF_MONTH)
                            )
                            
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                                    onValueChange = { },
                                    label = { Text("Date of Birth") },
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { dobDatePicker.show() })
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { dobDatePicker.show() },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        
                        if (showBlood) {
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = bloodGroupExpanded,
                                    onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = bloodGroup.ifEmpty { "Select Group" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Blood Group*") },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                                        colors = textFieldColors,
                                        isError = showValidationErrors && bloodGroup.isBlank(),
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = bloodGroupExpanded, onDismissRequest = { bloodGroupExpanded = false }) {
                                        bloodGroups.forEach { selection ->
                                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                                bloodGroup = selection
                                                bloodGroupExpanded = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (showBlood && showValidationErrors && bloodGroup.isBlank()) {
                        Text("Blood Group is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
            }

            // 2. Contact Details Card
            Spacer(Modifier.height(8.dp))
            FormSectionCard(
                title = "Contact Details",
                icon = Icons.Default.Phone
            ) {
                // Email (Pre-filled and read-only)
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Email*") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(Modifier.height(12.dp))

                // Mobile 1 only
                OutlinedTextField(
                    value = mobile1,
                    onValueChange = { mobile1 = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mobile 1*") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = showValidationErrors && !isValidMobile(mobile1)
                )
                
                if (showValidationErrors && !isValidMobile(mobile1)) {
                    Text("Enter valid mobile", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
            }

            // 3. Official Details Card
            Spacer(Modifier.height(8.dp))
            FormSectionCard(
                title = "Official Position",
                icon = Icons.Default.Work
            ) {
                // Row 1: Unit & District
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(0.4f)) {
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = !unitExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = unit.ifEmpty { "Unit" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                colors = textFieldColors,
                                isError = showValidationErrors && unit.isBlank(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                filteredUnits.forEach { selection ->
                                    DropdownMenuItem(text = { Text(selection) }, onClick = {
                                        unit = selection
                                        unitExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    if (!isHighRankingOfficer && !isSpecialUnit) {
                        val isDistrictLocked = availableDistricts.size == 1 && district.isNotBlank()
                        Box(modifier = Modifier.weight(0.6f)) {
                            ExposedDropdownMenuBox(
                                expanded = districtExpanded && !isDistrictLocked,
                                onExpandedChange = {
                                    if (!isSelfEdit && !isDistrictLocked) districtExpanded = !districtExpanded
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = district.ifEmpty { if (isSelfEdit) district else "Select District" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("District*") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    enabled = !isDistrictLocked,
                                    colors = if (isDistrictLocked) {
                                        OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                        )
                                    } else textFieldColors,
                                    trailingIcon = {
                                        if (!isDistrictLocked) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded)
                                        }
                                    },
                                    isError = showValidationErrors && district.isBlank() && !isHighRankingOfficer
                                )
                                if (!isSelfEdit && !isDistrictLocked) {
                                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                                        availableDistricts.forEach { selection ->
                                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                                if (district != selection) station = ""
                                                district = selection
                                                districtExpanded = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (showValidationErrors && unit.isBlank()) {
                    Text("Unit is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
                if (showValidationErrors && district.isBlank() && !isHighRankingOfficer && !isSpecialUnit) {
                    Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                Spacer(Modifier.height(12.dp))

                // Row 2: KGID / ID & Rank
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val showKgid = !isAutoAgid && isFieldVisible("kgid")
                    if (showKgid) {
                        OutlinedTextField(
                            value = kgid,
                            onValueChange = { newValue ->
                                kgid = when {
                                    isOfficer || isHighRankingOfficer -> newValue
                                    newValue.all { it.isDigit() } -> newValue
                                    else -> kgid
                                }
                            },
                            label = { Text(identifierLabel) },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                            colors = textFieldColors,
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = if (isOfficer || isHighRankingOfficer) KeyboardType.Text else KeyboardType.Number),
                            isError = showValidationErrors && !isKgidValid(kgid),
                            enabled = (isAdmin || isRegistration) && !isEdit
                        )
                    }

                    Box(modifier = Modifier.weight(0.4f)) {
                        ExposedDropdownMenuBox(
                            expanded = rankExpanded,
                            onExpandedChange = { rankExpanded = !rankExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = rank,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Rank*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text(if (unit.isBlank()) "Select Unit First" else "Select Rank", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                                colors = textFieldColors,
                                isError = showValidationErrors && rank.isBlank(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                                filteredRanks.forEach { selection ->
                                    DropdownMenuItem(text = { Text(selection) }, onClick = {
                                        rank = selection
                                        if (!ranksRequiringMetalNumber.contains(selection)) metalNumber = ""
                                        if (ministerialRanks.any { it.equals(selection, ignoreCase = true) }) {
                                            station = ""
                                        }
                                        if (highRankingOfficers.contains(selection)) {
                                            district = ""
                                            station = ""
                                        }
                                        rankExpanded = false
                                    })
                                }
                            }
                        }
                    }
                }
                
                if (showValidationErrors) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (!isAutoAgid && !isKgidValid(kgid) && isFieldVisible("kgid")) {
                            Text(if(isOfficer) "ID required" else "KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 4.dp, top = 4.dp))
                        }
                        if (rank.isBlank()) {
                            Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f).padding(start = 4.dp, top = 4.dp))
                        }
                    }
                }

                // Metal Number (conditional)
                if (showMetalNumberField && !isOfficer && isFieldVisible("metalNumber")) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = metalNumber,
                        onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Metal No") },
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showValidationErrors && metalNumber.isBlank()
                    )
                    if (showValidationErrors && metalNumber.isBlank()) {
                        Text("Metal no. required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }

                // Row 3: Station / Section
                if (!isHighRankingOfficer && !isDistrictLevelUnit && !isSpecialUnit && !isMinisterial) {
                    val filteredStations = remember(stationsForSelectedDistrict, rank, policeStationRanks) {
                        val isPoliceStationRank = policeStationRanks.any { it.equals(rank, ignoreCase = true) }
                        if (isPoliceStationRank) {
                            stationsForSelectedDistrict.filter { 
                                !it.contains("PS", ignoreCase = true) || it.contains(" PS", ignoreCase = true) 
                            }
                        } else {
                            stationsForSelectedDistrict
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = stationExpanded,
                                onExpandedChange = {
                                    if (district.isNotBlank() && filteredStations.isNotEmpty()) stationExpanded = !stationExpanded
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = station.ifEmpty { 
                                        if (district.isNotBlank() || stationsForSelectedDistrict.isNotEmpty()) "Select $stationLabel" 
                                        else "Select District First" 
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stationLabel) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                                    enabled = district.isNotBlank() && filteredStations.isNotEmpty(),
                                    colors = textFieldColors,
                                    isError = showValidationErrors && station.isBlank(),
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                                    filteredStations.forEach { selection ->
                                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                                            station = selection
                                            stationExpanded = false
                                        })
                                    }
                                }
                            }
                        }

                        if (station == "Others") {
                            OutlinedTextField(
                                value = manualSection,
                                onValueChange = { manualSection = it },
                                label = { Text("Specify Name*") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                isError = showValidationErrors && manualSection.isBlank(),
                                singleLine = true
                            )
                        }
                    }
                    
                    if (showValidationErrors) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (station.isBlank()) {
                                Text("$stationLabel required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 4.dp, top = 4.dp))
                            }
                            if (station == "Others" && manualSection.isBlank()) {
                                Text("Name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 4.dp, top = 4.dp))
                            }
                        }
                    }
                }

                // Row 4: Duty Role / Sub-Section & Date of Appointment (DOA)
                val registrationDutyRoles = remember(unit, subSectionList, fullUnits) {
                    constantsViewModel.getDutyRolesForUnit(unit, isRegistration)
                }
                val showDutyRole = registrationDutyRoles.isNotEmpty()
                val showDoa = isFieldVisible("doa")
                
                if (showDutyRole || showDoa) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showDutyRole) {
                            Box(modifier = Modifier.weight(1.1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = subSectionExpanded,
                                    onExpandedChange = { subSectionExpanded = !subSectionExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = subSection.ifEmpty { "Select Duty Role" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Duty Role / Sub-Section") },
                                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subSectionExpanded) },
                                        colors = textFieldColors,
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = subSectionExpanded, onDismissRequest = { subSectionExpanded = false }) {
                                        registrationDutyRoles.forEach { selection ->
                                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                                subSection = selection
                                                subSectionExpanded = false
                                            })
                                        }
                                        if (subSection.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("None", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    subSection = ""
                                                    subSectionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showDoa) {
                            val apptCalendar = Calendar.getInstance()
                            serviceStartDate?.let { apptCalendar.time = it }
                            val apptDatePicker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    serviceStartDate = cal.time
                                },
                                apptCalendar.get(Calendar.YEAR),
                                apptCalendar.get(Calendar.MONTH),
                                apptCalendar.get(Calendar.DAY_OF_MONTH)
                            )
                            
                            Box(modifier = Modifier.weight(0.9f)) {
                                OutlinedTextField(
                                    value = serviceStartDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                                    onValueChange = { },
                                    label = { Text("Appt Date") },
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { apptDatePicker.show() })
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { apptDatePicker.show() },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Security PIN Card
            if (!isOfficer && isFieldVisible("pin")) {
                Spacer(Modifier.height(8.dp))
                FormSectionCard(
                    title = "Security PIN",
                    icon = Icons.Default.Lock
                ) {
                    Text(
                        text = "Create a 6-digit PIN to secure your profile and login access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                            label = { Text("Create PIN*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                val image = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(image, contentDescription = if (pinVisible) "Hide PIN" else "Show PIN")
                                }
                            },
                            visualTransformation = if (pinVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = showValidationErrors && (pin.length != 6 || (pin.isNotEmpty() && pin != confirmPin)),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                            label = { Text("Confirm PIN*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                val image = if (confirmPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { confirmPinVisible = !confirmPinVisible }) {
                                    Icon(image, contentDescription = if (confirmPinVisible) "Hide PIN" else "Show PIN")
                                }
                            },
                            visualTransformation = if (confirmPinVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = showValidationErrors && (confirmPin.length != 6 || (confirmPin.isNotEmpty() && pin != confirmPin)),
                            singleLine = true
                        )
                    }
                    if (showValidationErrors && (pin.length != 6 || pin != confirmPin)) {
                        Text("PIN must be 6 digits and match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
            }

            // 5. Terms & Submission Card
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = acceptedTerms, 
                            onCheckedChange = { acceptedTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("I accept ")
                            Text(
                                text = "Terms & Conditions",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable {
                                    onNavigateToTerms?.invoke()
                                }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isSubmitting || isLoading) return@Button
                            isSubmitting = true
                            showValidationErrors = true
                            
                            val isEmailValid = if (!isFieldVisible("email")) true else if (isOfficer && email.isNullOrBlank()) true else isValidEmail(email ?: "")
                            if (!isEmailValid || !isValidMobile(mobile1) || name.isBlank()) {
                                Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (!isSelfEdit && !isAutoAgid && kgid.isBlank() && isFieldVisible("kgid")) {
                                Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (rank.isBlank()) {
                                Toast.makeText(context, "Rank is required", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (showMetalNumberField && metalNumber.isBlank() && !isOfficer && isFieldVisible("metalNumber")) {
                                Toast.makeText(context, "Metal number is required for this rank", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (district.isBlank() && !isHighRankingOfficer) {
                                Toast.makeText(context, "District is required", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (station.isBlank() && !isHighRankingOfficer && !isDistrictLevelUnit && !isMinisterial) {
                                Toast.makeText(context, "${if(unit == "State INT" || unitSections.isNotEmpty()) "Section" else "Station"} is required", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (station == "Others" && manualSection.isBlank()) {
                                Toast.makeText(context, "Please specify your section name", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (bloodGroup.isBlank() && !isOfficer && isFieldVisible("bloodGroup")) {
                                Toast.makeText(context, "Blood Group is required", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (!isOfficer && isFieldVisible("pin") && (pin.length != 6 || pin != confirmPin)) {
                                Toast.makeText(context, "PIN mismatch or invalid", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (!acceptedTerms) {
                                Toast.makeText(context, "Accept terms to continue", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }
                            if (subSection == "Others" && manualSubSection.isBlank()) {
                                Toast.makeText(context, "Please specify your duty role", Toast.LENGTH_SHORT).show()
                                isSubmitting = false
                                return@Button
                            }

                            val finalKgid = if (kgid.isBlank()) "TEMP-${System.currentTimeMillis()}" else kgid
                            val isManual = station == "Others"

                            val emp = Employee(
                                kgid = finalKgid,
                                name = name.trim(),
                                email = email.trim().lowercase(),
                                mobile1 = mobile1.trim(),
                                mobile2 = mobile2.trim().takeIf { it.isNotBlank() },
                                landline = landline.trim().takeIf { it.isNotBlank() },
                                landline2 = landline2.trim().takeIf { it.isNotBlank() },
                                rank = rank.trim(),
                                district = district.trim(),
                                station = when {
                                    isManual -> manualSection.trim()
                                    station.isNotBlank() -> station.trim()
                                    isDistrictLevelUnit -> district.trim()
                                    else -> ""
                                },
                                unit = unit.trim().takeIf { it.isNotBlank() },
                                bloodGroup = bloodGroup.ifBlank { null },
                                metalNumber = metalNumber.trim().takeIf { it.isNotBlank() },
                                isAdmin = initialEmployee?.isAdmin ?: false,
                                photoUrl = croppedPhotoUri?.toString() ?: currentPhotoUrl,
                                isManualStation = isManual,
                                gender = gender,
                                serviceStartDate = serviceStartDate,
                                dateOfBirth = dateOfBirth,
                                subSection = if (subSection == "Others") manualSubSection.trim() else subSection.trim().takeIf { it.isNotBlank() },
                                isManualSubSection = subSection == "Others"
                            )

                            android.util.Log.d("CommonEmployeeForm", "🚀 Launching coroutine for submission...")
                            coroutineScope.launch {
                                try {
                                    val pending = PendingRegistrationEntity(
                                        name = emp.name,
                                        kgid = emp.kgid,
                                        email = emp.email,
                                        mobile1 = emp.mobile1 ?: "",
                                        mobile2 = emp.mobile2,
                                        landline = emp.landline,
                                        landline2 = emp.landline2,
                                        pin = pin,
                                        rank = emp.rank ?: "",
                                        metalNumber = emp.metalNumber,
                                        district = emp.district.orEmpty(),
                                        station = if (station == "Others") manualSection.trim() else emp.station.orEmpty(),
                                        unit = emp.unit,
                                        bloodGroup = emp.bloodGroup.orEmpty(),
                                        firebaseUid = "",
                                        photoUrl = emp.photoUrl,
                                        isManualStation = emp.isManualStation,
                                        gender = emp.gender,
                                        serviceStartDate = emp.serviceStartDate,
                                        dateOfBirth = emp.dateOfBirth,
                                        subSection = emp.subSection,
                                        isManualSubSection = emp.isManualSubSection
                                    )
                                    onRegisterSubmit?.invoke(pending, croppedPhotoUri)
                                    delay(3000)
                                    isSubmitting = false
                                } catch (e: Exception) {
                                    android.util.Log.e("CommonEmployeeForm", "Submission error: ${e.message}", e)
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting || isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Submitting...")
                            }
                        } else {
                            Text("Submit for approval", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

        } else {
            // ==================== EDIT / ADMIN FORM ====================

            // 1. Personal Info Card
            FormSectionCard(
                title = "Personal Information",
                icon = Icons.Default.Person
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name*") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    isError = showValidationErrors && name.isBlank()
                )
                if (showValidationErrors && name.isBlank()) {
                    Text("Name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                Spacer(Modifier.height(12.dp))

                // Gender Selection (Full Width)
                if (isFieldVisible("gender")) {
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender*") },
                            leadingIcon = { Icon(Icons.Default.Face, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                            listOf("Male", "Female").forEach { selection ->
                                DropdownMenuItem(text = { Text(selection) }, onClick = {
                                    gender = selection
                                    genderExpanded = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Row for Date of Birth & Blood Group
                val showDob = isFieldVisible("dob")
                val showBlood = !isOfficer && isFieldVisible("bloodGroup")
                if (showDob || showBlood) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showDob) {
                            val dobCalendar = Calendar.getInstance()
                            dateOfBirth?.let { dobCalendar.time = it }
                            val dobDatePicker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    dateOfBirth = cal.time
                                },
                                dobCalendar.get(Calendar.YEAR),
                                dobCalendar.get(Calendar.MONTH),
                                dobCalendar.get(Calendar.DAY_OF_MONTH)
                            )
                            
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                                    onValueChange = { },
                                    label = { Text("Date of Birth") },
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { dobDatePicker.show() })
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { dobDatePicker.show() },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        
                        if (showBlood) {
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = bloodGroupExpanded,
                                    onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = bloodGroup.ifEmpty { "Select Group" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Blood Group*") },
                                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                                        colors = textFieldColors,
                                        isError = showValidationErrors && bloodGroup.isBlank(),
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = bloodGroupExpanded, onDismissRequest = { bloodGroupExpanded = false }) {
                                        bloodGroups.forEach { selection ->
                                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                                bloodGroup = selection
                                                bloodGroupExpanded = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (showBlood && showValidationErrors && bloodGroup.isBlank()) {
                        Text("Blood Group is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
            }

            // 2. Contact Details Card
            Spacer(Modifier.height(8.dp))
            FormSectionCard(
                title = "Contact Details",
                icon = Icons.Default.Phone
            ) {
                // Email field (conditional)
                if (isFieldVisible("email")) {
                    OutlinedTextField(
                        value = email ?: "",
                        onValueChange = { email = it },
                        label = { Text(if (isOfficer) "Email (Optional)" else "Email*") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = showValidationErrors && !isOfficer && !isValidEmail(email ?: "")
                    )
                    if (showValidationErrors && !isOfficer && !isValidEmail(email ?: "")) {
                        Text("Enter valid email", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Mobile 1 only
                OutlinedTextField(
                    value = mobile1,
                    onValueChange = { mobile1 = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mobile 1*") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = showValidationErrors && !isValidMobile(mobile1)
                )
                if (showValidationErrors && !isValidMobile(mobile1)) {
                    Text("Enter valid mobile", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                // Landlines (conditional)
                val showLandline1 = isFieldVisible("landline")
                val showLandline2 = isFieldVisible("landline2")
                if (showLandline1 || showLandline2) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showLandline1) {
                            OutlinedTextField(
                                value = landline,
                                onValueChange = { landline = it.filter { ch -> ch.isDigit() || ch == '-' } },
                                label = { Text("Landline (Opt)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                        if (showLandline2) {
                            OutlinedTextField(
                                value = landline2,
                                onValueChange = { landline2 = it.filter { ch -> ch.isDigit() || ch == '-' } },
                                label = { Text("Landline 2 (Opt)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                    }
                }
            }

            // 3. Official Details Card
            Spacer(Modifier.height(8.dp))
            FormSectionCard(
                title = "Official Position",
                icon = Icons.Default.Work
            ) {
                // KGID Field (admin only)
                if (!isSelfEdit && !isAutoAgid && isFieldVisible("kgid")) {
                    OutlinedTextField(
                        value = kgid,
                        onValueChange = { newValue ->
                            kgid = when {
                                isOfficer || isHighRankingOfficer -> newValue
                                newValue.all { ch -> ch.isDigit() } -> newValue
                                else -> kgid
                            }
                        },
                        label = { Text(identifierLabel) },
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = if (isOfficer || isHighRankingOfficer) KeyboardType.Text else KeyboardType.Number),
                        isError = showValidationErrors && !isKgidValid(kgid),
                        enabled = (isAdmin || isRegistration) && !isEdit
                    )
                    if (showValidationErrors && !isKgidValid(kgid)) {
                        Text(if(isOfficer || isHighRankingOfficer) "ID required" else "KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Row: Unit & District
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(0.4f)) {
                        ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = !unitExpanded }) {
                            OutlinedTextField(
                                value = unit.ifEmpty { "Unit" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                colors = textFieldColors,
                                isError = showValidationErrors && unit.isBlank(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                                filteredUnits.forEach { selection ->
                                    DropdownMenuItem(text = { Text(selection) }, onClick = {
                                        unit = selection
                                        unitExpanded = false
                                    })
                                }
                            }
                        }
                    }

                    if (!isHighRankingOfficer && !isSpecialUnit) {
                        val isDistrictLocked = availableDistricts.size == 1 && district.isNotBlank()
                        Box(modifier = Modifier.weight(0.6f)) {
                            ExposedDropdownMenuBox(expanded = districtExpanded && !isDistrictLocked, onExpandedChange = {
                                if (!isSelfEdit && !isDistrictLocked) districtExpanded = !districtExpanded
                            }) {
                                OutlinedTextField(
                                    value = district.ifEmpty { if (isSelfEdit) district else "Select District" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("District*") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    enabled = !isDistrictLocked,
                                    colors = if (isDistrictLocked) {
                                        OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                        )
                                    } else textFieldColors,
                                    trailingIcon = { 
                                        if (!isDistrictLocked) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded)
                                        }
                                    },
                                    isError = showValidationErrors && district.isBlank()
                                )
                                if (!isSelfEdit && !isDistrictLocked) {
                                    ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                                        availableDistricts.forEach { selection ->
                                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                                if (district != selection) station = ""
                                                district = selection
                                                districtExpanded = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (showValidationErrors && unit.isBlank()) {
                    Text("Unit required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }
                if (showValidationErrors && district.isBlank() && !isHighRankingOfficer && !isSpecialUnit) {
                    Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                Spacer(Modifier.height(12.dp))

                // Rank selection
                ExposedDropdownMenuBox(expanded = rankExpanded, onExpandedChange = { rankExpanded = !rankExpanded }) {
                    OutlinedTextField(
                        value = rank.ifEmpty { "Select Rank" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank*") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                        colors = textFieldColors,
                        isError = showValidationErrors && rank.isBlank(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                        filteredRanks.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                rank = selection
                                if (!ranksRequiringMetalNumber.contains(selection)) metalNumber = ""
                                if (ministerialRanks.any { it.equals(selection, ignoreCase = true) }) {
                                    station = ""
                                }
                                if (highRankingOfficers.contains(selection)) {
                                    district = ""
                                    station = ""
                                }
                                rankExpanded = false
                            })
                        }
                    }
                }
                if (showValidationErrors && rank.isBlank()) {
                    Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                // Metal number
                if (showMetalNumberField && !isOfficer) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = metalNumber,
                        onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Metal Number") },
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showValidationErrors && metalNumber.isBlank()
                    )
                    if (showValidationErrors && metalNumber.isBlank()) {
                        Text("Metal number required for this rank", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }

                // Row: Station / Section
                val hasSectionsEdit = remember(unitSections, unit, district) {
                    unitSections.isNotEmpty() || unit == "State INT" || district == "HQ"
                }

                if (!isHighRankingOfficer && (!isDistrictLevelUnit || hasSectionsEdit) && !isSpecialUnit) {
                    val filteredStations = remember(stationsForSelectedDistrict, rank, policeStationRanks, unit, unitSections) {
                        if (unitSections.isNotEmpty()) {
                            unitSections + listOf("Others")
                        } else if (unit == "State INT") {
                             Constants.stateIntSections + listOf("Others")
                        } else {
                            val isPoliceStationRank = policeStationRanks.contains(rank)
                            val baseStations = if (isPoliceStationRank) {
                                stationsForSelectedDistrict
                            } else {
                                stationsForSelectedDistrict.filter { !it.endsWith(" PS", ignoreCase = true) }
                            }
                            if (hasSections || district == "HQ") {
                                baseStations + listOf("Others")
                            } else {
                                baseStations
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(expanded = stationExpanded, onExpandedChange = {
                                if ((district.isNotBlank() || hasSections) && filteredStations.isNotEmpty()) stationExpanded = !stationExpanded
                            }) {
                                OutlinedTextField(
                                    value = station.ifEmpty { 
                                        if (hasSections) "Select Section" 
                                        else if (district.isNotBlank()) "Select Station" 
                                        else "Select District First" 
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stationLabel) },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                                    enabled = (district.isNotBlank() || hasSections) && filteredStations.isNotEmpty(),
                                    colors = textFieldColors,
                                    isError = showValidationErrors && station.isBlank(),
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(expanded = stationExpanded, onDismissRequest = { stationExpanded = false }) {
                                    filteredStations.forEach { selection ->
                                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                                            station = selection
                                            stationExpanded = false
                                        })
                                    }
                                }
                            }
                        }

                        if (station == "Others") {
                            OutlinedTextField(
                                value = manualSection,
                                onValueChange = { manualSection = it },
                                label = { Text("Specify ${if (hasSections) "Section" else "Station"} Name*") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                colors = textFieldColors,
                                modifier = Modifier.weight(1f),
                                isError = showValidationErrors && manualSection.isBlank()
                            )
                        }
                    }
                    
                    if (showValidationErrors) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (station.isBlank()) {
                                Text(if (hasSections) "Section required" else "Station required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 4.dp, top = 4.dp))
                            }
                            if (station == "Others" && manualSection.isBlank()) {
                                Text("Please specify name", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(start = 4.dp, top = 4.dp))
                            }
                        }
                    }
                }

                // Row: Duty Role & Date of Appointment (DOA)
                val otherDutyRoles = remember(unit, subSectionList, fullUnits) {
                    constantsViewModel.getDutyRolesForUnit(unit, isRegistration)
                }
                val showDutyRole = otherDutyRoles.isNotEmpty()
                val showDoa = isFieldVisible("doa")
                
                if (showDutyRole || showDoa) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showDutyRole) {
                            Box(modifier = Modifier.weight(1.1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = subSectionExpanded,
                                    onExpandedChange = { subSectionExpanded = !subSectionExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = subSection.ifEmpty { "Select Duty Role" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Duty Role / Sub-Section") },
                                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subSectionExpanded) },
                                        colors = textFieldColors,
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(expanded = subSectionExpanded, onDismissRequest = { subSectionExpanded = false }) {
                                        otherDutyRoles.forEach { selection ->
                                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                                subSection = selection
                                                subSectionExpanded = false
                                            })
                                        }
                                        if (subSection.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("None", color = MaterialTheme.colorScheme.error) },
                                                onClick = {
                                                    subSection = ""
                                                    subSectionExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (showDoa) {
                            val apptCalendar = Calendar.getInstance()
                            serviceStartDate?.let { apptCalendar.time = it }
                            val apptDatePicker = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, dayOfMonth)
                                    serviceStartDate = cal.time
                                },
                                apptCalendar.get(Calendar.YEAR),
                                apptCalendar.get(Calendar.MONTH),
                                apptCalendar.get(Calendar.DAY_OF_MONTH)
                            )
                            
                            Box(modifier = Modifier.weight(0.9f)) {
                                OutlinedTextField(
                                    value = serviceStartDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                                    onValueChange = { },
                                    label = { Text("Appt Date") },
                                    readOnly = true,
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { apptDatePicker.show() })
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { apptDatePicker.show() },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }

                // Handle manual entry for Duty Role below the row if "Others" is selected
                if (subSection == "Others") {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualSubSection,
                        onValueChange = { manualSubSection = it },
                        label = { Text("Specify Duty Role*") },
                        placeholder = { Text("Duty Role Name") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        isError = showValidationErrors && manualSubSection.isBlank(),
                        singleLine = true
                    )
                    if (showValidationErrors && manualSubSection.isBlank()) {
                        Text("Duty role name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }
                }
            }

            // 4. Submission Button (Simple for Admin/Self-Edit)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isSubmitting || isLoading) return@Button
                    isSubmitting = true
                    showValidationErrors = true
                    
                    val isEmailValid = if (!isFieldVisible("email")) true else if (isOfficer && email.isNullOrBlank()) true else isValidEmail(email ?: "")
                    if (!isEmailValid || !isValidMobile(mobile1) || name.isBlank()) {
                        Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (!isSelfEdit && !isAutoAgid && kgid.isBlank() && isFieldVisible("kgid")) {
                        Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (rank.isBlank()) {
                        Toast.makeText(context, "Rank is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (showMetalNumberField && metalNumber.isBlank() && !isOfficer && isFieldVisible("metalNumber")) {
                        Toast.makeText(context, "Metal number is required for this rank", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (district.isBlank() && !isHighRankingOfficer) {
                        Toast.makeText(context, "District is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (station.isBlank() && !isHighRankingOfficer && !isDistrictLevelUnit && !isMinisterial) {
                        Toast.makeText(context, "${if(unit == "State INT" || unitSections.isNotEmpty()) "Section" else "Station"} is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (station == "Others" && manualSection.isBlank()) {
                        Toast.makeText(context, "Please specify your section name", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (bloodGroup.isBlank() && !isOfficer && isFieldVisible("bloodGroup")) {
                        Toast.makeText(context, "Blood Group is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }
                    if (subSection == "Others" && manualSubSection.isBlank()) {
                        Toast.makeText(context, "Please specify your duty role", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        return@Button
                    }

                    val finalKgid = if (kgid.isBlank()) "TEMP-${System.currentTimeMillis()}" else kgid
                    val isManual = station == "Others"

                    val emp = Employee(
                        kgid = finalKgid,
                        name = name.trim(),
                        email = email?.trim()?.lowercase() ?: "",
                        mobile1 = mobile1.trim(),
                        mobile2 = mobile2.trim().takeIf { it.isNotBlank() },
                        landline = landline.trim().takeIf { it.isNotBlank() },
                        landline2 = landline2.trim().takeIf { it.isNotBlank() },
                        rank = rank.trim(),
                        district = district.trim(),
                        station = when {
                            isManual -> manualSection.trim()
                            station.isNotBlank() -> station.trim()
                            isDistrictLevelUnit -> district.trim()
                            else -> ""
                        },
                        unit = unit.trim().takeIf { it.isNotBlank() },
                        bloodGroup = bloodGroup.ifBlank { null },
                        metalNumber = metalNumber.trim().takeIf { it.isNotBlank() },
                        isAdmin = initialEmployee?.isAdmin ?: false,
                        photoUrl = croppedPhotoUri?.toString() ?: currentPhotoUrl,
                        isManualStation = isManual,
                        gender = gender,
                        serviceStartDate = serviceStartDate,
                        dateOfBirth = dateOfBirth,
                        subSection = if (subSection == "Others") manualSubSection.trim() else subSection.trim().takeIf { it.isNotBlank() },
                        isManualSubSection = subSection == "Others"
                    )

                    android.util.Log.d("CommonEmployeeForm", "🚀 Launching coroutine for submission...")
                    coroutineScope.launch {
                        try {
                            onSubmit(emp, croppedPhotoUri)
                            delay(3000)
                            isSubmitting = false
                        } catch (e: Exception) {
                            android.util.Log.e("CommonEmployeeForm", "Submission error: ${e.message}", e)
                            isSubmitting = false
                        }
                    }
                },
                enabled = !isSubmitting && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting || isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Submitting...")
                    }
                } else {
                    Text(
                        text = if (initialEmployee != null) "Submit update for approval" else "Submit for approval",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Linear Progress Indicator for submit / load in progress (Shared layout)
        if (isSubmitting || isLoading) {
            val statusTitle = if (isLoading) "Uploading details to server..." else "Preparing submission..."
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Image chooser dialog (Preserving launchCamera() call!)
        if (showSourceDialog) {
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text("Select Image Source") },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSourceDialog = false
                                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        launchCamera()
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                            Spacer(Modifier.width(12.dp))
                            Text("Camera")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSourceDialog = false
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                            Spacer(Modifier.width(12.dp))
                            Text("Gallery")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSourceDialog = false }) { Text("Close") }
                }
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}

/* util functions */
private fun launchUCrop(context: Context, sourceUri: Uri, launcher: ActivityResultLauncher<Intent>) {
    try {
        val destFile = File(context.cacheDir, "ucrop_${UUID.randomUUID()}.jpg")
        val destUri = try {
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                destFile
            )
        } catch (e: Exception) {
            android.util.Log.e("CommonEmployeeForm", "FileProvider error: ${e.message}", e)
            Toast.makeText(context, "Failed to access file provider", Toast.LENGTH_SHORT).show()
            return
        }

        val options = UCrop.Options().apply {
            setToolbarTitle("Crop Image")
            setCircleDimmedLayer(true)
            setFreeStyleCropEnabled(false)
            setCompressionQuality(90)
            
            // Fix Color Overlap - Match App Theme
            setToolbarColor(androidx.core.content.ContextCompat.getColor(context, com.example.policemobiledirectory.R.color.md_theme_light_primary))
            setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(context, com.example.policemobiledirectory.R.color.md_theme_light_primary))
        }

        val intent = UCrop.of(sourceUri, destUri).withAspectRatio(1f, 1f).withOptions(options).getIntent(context)
        launcher.launch(intent)
    } catch (e: Exception) {
        android.util.Log.e("CommonEmployeeForm", "UCrop launch error: ${e.message}", e)
        Toast.makeText(context, "Failed to launch image cropper: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// Helper to create temp URI for camera
private fun createTempImageUri(context: Context): Uri? {
    return try {
        val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider", // Use dynamic package name for Admin/User flavors
            file
        )
    } catch (e: Exception) {
        android.util.Log.e("CommonEmployeeForm", "Error creating temp file URI: ${e.message}", e)
        null
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
