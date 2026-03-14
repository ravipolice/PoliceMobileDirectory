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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
fun isKgidValid(v: String) = v.isNotBlank()

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

    // Get constants from ViewModel
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val bloodGroups by constantsViewModel.bloodGroups.collectAsStateWithLifecycle()

    val ranksRequiringMetalNumber by constantsViewModel.ranksRequiringMetalNumber.collectAsStateWithLifecycle()

    val ministerialRanks by constantsViewModel.ministerialRanks.collectAsStateWithLifecycle()
    val policeStationRanks by constantsViewModel.policeStationRanks.collectAsStateWithLifecycle()
    val highRankingOfficers by constantsViewModel.highRankingOfficers.collectAsStateWithLifecycle()
    val units by constantsViewModel.units.collectAsStateWithLifecycle()
    val fullUnits by constantsViewModel.fullUnits.collectAsStateWithLifecycle() // ✅ Need full objects for hidden flag
    val globalHiddenFields by constantsViewModel.globalHiddenFields.collectAsStateWithLifecycle() // ✅ Global Hidden Fields
    val ksrpBattalions by constantsViewModel.ksrpBattalions.collectAsStateWithLifecycle()

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

    // Helper to check if field is visible (Hybrid Logic: Global OR Unit)
    // Returns TRUE if field is NOT hidden in Global AND NOT hidden in Unit
    val isFieldVisible: (String) -> Boolean = { fieldId ->
        val isHiddenGlobally = globalHiddenFields.contains(fieldId)
        val isHiddenInUnit = selectedUnitModel?.hiddenFields?.contains(fieldId) == true
        
        !isHiddenGlobally && !isHiddenInUnit
    }

    // UI states
    var rankExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var stationExpanded by remember { mutableStateOf(false) }
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

    // Reset station when district changes
    LaunchedEffect(district) {
        if (district.isNotBlank() && station.isNotBlank()) {
             station = ""
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

    // 🔹 CENTRALIZED STATIONS & SECTIONS LOGIC
    val stationsForSelectedDistrict by produceState<List<String>>(initialValue = emptyList(), key1 = unit, key2 = district) {
        value = constantsViewModel.getStationsAndSectionsForUnit(unit, district)
    }

    // Identify if label should be "Station" or "Section"
    val isDistrictLevelByModel by produceState(initialValue = false, key1 = unit) {
        value = constantsViewModel.isDistrictLevelUnit(unit)
    }
    val useStationLabel = remember(unit, isDistrictLevelByModel) {
        unit == "Law & Order" || isDistrictLevelByModel
    }

    // Refactored for readability as per review
    // Standardized resolution via ViewModel
    val stationsForSelectedDistrictActual = stationsForSelectedDistrict // Placeholder for better naming if needed
    

    // Validate Station Selection when options change (e.g. Unit change filters stations)
    LaunchedEffect(stationsForSelectedDistrict) {
         if (station.isNotBlank() && station != "Others" && !stationsForSelectedDistrict.contains(station)) {
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
        // photo
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showSourceDialog = true },
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
                        // Edit icon overlay
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Photo",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f), CircleShape)
                                .padding(8.dp)
                        )
                    }

                    !currentPhotoUrl.isNullOrBlank() -> {
                        Image(
                            painter = rememberAsyncImagePainter(currentPhotoUrl),
                            contentDescription = "Existing Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                         // Edit icon overlay for existing photo too
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Photo",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f), CircleShape)
                                .padding(8.dp)
                        )
                    }

                    else -> {
                        // Empty State
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Select Photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                "Add Photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(sectionSpacing))

        // Registration form custom order
        if (isRegistration) {
            // Row 2: Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name*") },
                modifier = Modifier.fillMaxWidth(),
                isError = showValidationErrors && name.isBlank()
            )
            if (showValidationErrors && name.isBlank()) {
                Text("Name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 3: Email
            if (isFieldVisible("email")) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = showValidationErrors && email.isNotBlank() && !isValidEmail(email)
                )
                if (showValidationErrors && email.isNotBlank() && !isValidEmail(email)) {
                    Text("Enter valid email", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(fieldSpacing))
            }

            // Row 4: Mobile 1
            OutlinedTextField(
                value = mobile1,
                onValueChange = { mobile1 = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mobile 1*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = showValidationErrors && !isValidMobile(mobile1)
            )
            if (showValidationErrors && !isValidMobile(mobile1)) {
                Text("Enter valid mobile", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 5: Mobile 2
            if (isFieldVisible("mobile2")) {
                OutlinedTextField(
                    value = mobile2,
                    onValueChange = { mobile2 = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Mobile 2 (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(fieldSpacing))
            }

            // Row 6: Landline
            if (isFieldVisible("landline")) {
                OutlinedTextField(
                    value = landline,
                    onValueChange = { landline = it.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text("Landline (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(fieldSpacing))
            }





            // Unit (moved up)
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = unit.ifEmpty { "Unit" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                    isError = showValidationErrors && unit.isBlank()
                )
                if (showValidationErrors && unit.isBlank()) {
                    Text("Unit required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                    units.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                            unit = selection
                            unitExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 6: KGID, Rank, Metal Number (conditional) - all in same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KGID / ID
                if (!isAutoAgidRank) {
                    OutlinedTextField(
                        value = kgid,
                        onValueChange = { 
                            // For Officers, allow any characters (AGID). Otherwise, only allow digits (KGID).
                            if (isOfficer || isHighRankingOfficer) kgid = it else if (it.all { ch -> ch.isDigit() }) kgid = it
                        },
                        label = { Text(if(isOfficer || isHighRankingOfficer) "Officer ID (AGID)*" else "KGID*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = if (isOfficer || isHighRankingOfficer) KeyboardType.Text else KeyboardType.Number),
                        isError = showValidationErrors && !isKgidValid(kgid),
                        enabled = (isAdmin || isRegistration) && !isEdit
                    )
                }

                // Rank
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = rank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank") },
                        placeholder = { Text("Select Rank (Optional)") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },

                    )
                    ExposedDropdownMenu(expanded = rankExpanded, onDismissRequest = { rankExpanded = false }) {
                        filteredRanks.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                rank = selection
                                if (!ranksRequiringMetalNumber.contains(selection)) metalNumber = ""
                                // If rank is ministerial, verify if we need to clear station or just let UI hide it
                                // Clearing it ensures validation passes if we accidentally had one selected
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

                // Metal Number (Dynamic)
                if (showMetalNumberField && !isOfficer && isFieldVisible("metalNumber")) {
                    OutlinedTextField(
                        value = metalNumber,
                        onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Metal No.*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showValidationErrors && metalNumber.isBlank()
                    )
                }

            }
            // Error messages below the row
            if (showValidationErrors) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (!isKgidValid(kgid)) {
                        Text(if(isOfficer) "ID required" else "KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    if (rank.isBlank()) {
                        Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    if (showMetalNumberField && metalNumber.isBlank() && !isOfficer) {
                        Text("Metal no. required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 7: Unit and District (Unit first, then District)
            val isSpecialUnit = remember(selectedUnitModel) {
                selectedUnitModel?.mappingType == "none"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Unit was here, moved up.
                Spacer(Modifier.width(1.dp)) // Placeholder to prevent empty Row issues if any, though District is aligned.


                // District
                if (!isHighRankingOfficer && !isSpecialUnit) {
                    ExposedDropdownMenuBox(
                        expanded = districtExpanded,
                        onExpandedChange = {
                            if (!isSelfEdit) districtExpanded = !districtExpanded
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = district.ifEmpty { if (isSelfEdit) district else "Select District" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("District*") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                            isError = showValidationErrors && district.isBlank()
                        )
                        if (!isSelfEdit) {
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
            if (showValidationErrors && district.isBlank() && !isHighRankingOfficer && !isSpecialUnit) {
                Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 8: Station/Section (Full Width)
            if (!isHighRankingOfficer && !isSpecialUnit && !isMinisterial) {
                val filteredStations = remember(stationsForSelectedDistrict, rank, policeStationRanks) {
                    val isPoliceStationRank = policeStationRanks.contains(rank)
                    // Apply rank-based filtering to reduce noise for PS ranks ONLY if useStationLabel is true
                    if (isPoliceStationRank && useStationLabel) {
                         stationsForSelectedDistrict.filter { it.contains(" PS", ignoreCase = true) || !it.contains(" PS", ignoreCase = true) }
                         // Actually, same logic as User flavor for consistency
                         stationsForSelectedDistrict.filter { s ->
                            val isPS = s.contains(" PS", ignoreCase = true)
                            if (isPoliceStationRank && s.contains(" PS", ignoreCase = true)) true
                            else if (!s.contains(" PS", ignoreCase = true)) true 
                            else false
                        }
                    } else {
                        stationsForSelectedDistrict
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = stationExpanded,
                        onExpandedChange = {
                            if (district.isNotBlank() && filteredStations.isNotEmpty()) stationExpanded = !stationExpanded
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = station.ifEmpty { 
                                if (stationsForSelectedDistrict.isNotEmpty()) "Select ${if(useStationLabel) "Station" else "Section"}" 
                                else "Select District First" 
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (useStationLabel) "Station *" else "Section / Branch *") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                            enabled = district.isNotBlank() && filteredStations.isNotEmpty(),
                            isError = showValidationErrors && station.isBlank()
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

                    // Manual Section Entry
                    if (station == "Others") {
                        OutlinedTextField(
                            value = manualSection,
                            onValueChange = { manualSection = it },
                            label = { Text("Specify Name*") },
                            placeholder = { Text("Section Name") },
                            modifier = Modifier.weight(1f),
                            isError = showValidationErrors && manualSection.isBlank(),
                            singleLine = true
                        )
                    }
                }
                
                if (showValidationErrors) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (station.isBlank() && stationsForSelectedDistrict.isNotEmpty()) {
                            Text(
                                if (useStationLabel) "Station required" else "Section required", 
                                color = MaterialTheme.colorScheme.error, 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                           Spacer(modifier = Modifier.weight(1f))
                        }
                        
                         if (station == "Others" && manualSection.isBlank()) {
                            Text(
                                "Name required", 
                                color = MaterialTheme.colorScheme.error, 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                        }
                    }
                }
            }




            Spacer(Modifier.height(fieldSpacing))

            // Blood Group row (Hide for Officer, Hide if configured hidden)
            if (!isOfficer && isFieldVisible("bloodGroup")) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = bloodGroupExpanded,
                        onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = bloodGroup.ifEmpty { "Blood Group" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) }
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
                if (showValidationErrors && station.isBlank() && !isSpecialUnit && !isMinisterial) {
                    Text("Station required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(fieldSpacing))
            }

            // KCSR Fields: Gender
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) }
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
                Spacer(Modifier.height(fieldSpacing))
            }

            // KCSR Fields: Date of Birth and Date of Appointment
            if (isFieldVisible("dob")) {
                val dobCalendar = Calendar.getInstance()
                dateOfBirth?.let { dobCalendar.time = it }

                val dobPickerDialog = DatePickerDialog(
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

                OutlinedTextField(
                    value = dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Date of Birth") },
                    modifier = Modifier.fillMaxWidth().clickable { dobPickerDialog.show() },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(fieldSpacing))
            }

            if (isFieldVisible("doa")) {
                val apptCalendar = Calendar.getInstance()
                serviceStartDate?.let { apptCalendar.time = it }

                val apptPickerDialog = DatePickerDialog(
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

                OutlinedTextField(
                    value = serviceStartDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Date of Appointment") },
                    modifier = Modifier.fillMaxWidth().clickable { apptPickerDialog.show() },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(fieldSpacing))
            }

            // Row 9: Create PIN, Confirm PIN (on same row) - Hide for Officer
            if (!isOfficer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("Create PIN*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = showValidationErrors && (pin.length != 6 || (pin.isNotEmpty() && pin != confirmPin))
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                        label = { Text("Confirm PIN*") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = showValidationErrors && (confirmPin.length != 6 || (confirmPin.isNotEmpty() && pin != confirmPin))
                    )
                }
                if (showValidationErrors && (pin.length != 6 || pin != confirmPin)) {
                    Text("PIN must be 6 digits and match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(fieldSpacing))
            }

            // Row 10: Terms and condition
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
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
            Spacer(Modifier.height(sectionSpacing))
        } else {
            // Non-registration form (admin/self-edit) - keep original order
            // KGID (admin & registration only)
            if (!isSelfEdit) {
                OutlinedTextField(
                    value = kgid,
                    onValueChange = { newValue ->
                        kgid = when {
                            isOfficer -> newValue
                            newValue.all { it.isDigit() } -> newValue
                            else -> kgid
                        }
                    },
                    label = { Text(if (isOfficer) "Officer ID*" else "KGID*") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (isOfficer) KeyboardType.Text else KeyboardType.Number),
                    isError = showValidationErrors && !isKgidValid(kgid),
                    enabled = (isAdmin || isRegistration) && !isEdit
                )
                if (showValidationErrors && !isKgidValid(kgid)) {
                    Text(if(isOfficer) "ID required" else "KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(fieldSpacing))
            }

            // Name
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name*") }, modifier = Modifier.fillMaxWidth(), isError = showValidationErrors && name.isBlank())
            Spacer(Modifier.height(fieldSpacing))

            // Email (Optional/Hidden for Officers if needed, but let's keep it optional)
            if (isFieldVisible("email")) {
                OutlinedTextField(
                    value = email ?: "",
                    onValueChange = { email = it },
                    label = { Text(if (isOfficer) "Email (Optional)" else "Email*") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = showValidationErrors && !isOfficer && !isValidEmail(email ?: "")
                )
                if (showValidationErrors && !isOfficer && !isValidEmail(email ?: "")) Text("Enter valid email", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(fieldSpacing))
            }

            // Mobile1
            OutlinedTextField(
                value = mobile1,
                onValueChange = { mobile1 = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mobile 1*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = showValidationErrors && !isValidMobile(mobile1)
            )
            if (showValidationErrors && !isValidMobile(mobile1)) Text("Enter valid mobile", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Mobile2
            if (isFieldVisible("mobile2")) {
                OutlinedTextField(value = mobile2, onValueChange = { mobile2 = it.filter { ch -> ch.isDigit() } }, label = { Text("Mobile 2 (Optional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(Modifier.height(fieldSpacing))
            }

            // Landline
            if (isFieldVisible("landline")) {
                OutlinedTextField(value = landline, onValueChange = { landline = it.filter { ch -> ch.isDigit() || ch == '-' } }, label = { Text("Landline (Optional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(Modifier.height(fieldSpacing))
            }

            // Landline 2
            if (isFieldVisible("landline2")) {
                OutlinedTextField(value = landline2, onValueChange = { landline2 = it.filter { ch -> ch.isDigit() || ch == '-' } }, label = { Text("Landline 2 (Optional)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(Modifier.height(fieldSpacing))
            }

            // Rank
            ExposedDropdownMenuBox(expanded = rankExpanded, onExpandedChange = { rankExpanded = !rankExpanded }) {
                OutlinedTextField(
                    value = rank.ifEmpty { "Select Rank" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rank*") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                    isError = showValidationErrors && rank.isBlank()
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
            if (showValidationErrors && rank.isBlank()) Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(fieldSpacing))

            // Metal number
            if (showMetalNumberField && !isOfficer) {
                OutlinedTextField(
                    value = metalNumber,
                    onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Metal Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && metalNumber.isBlank()
                )
                if (showValidationErrors && metalNumber.isBlank()) Text("Metal number required for this rank", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(fieldSpacing))
            }

            // Unit (Moved here for proper dependency flow: Unit -> District -> Station)
            ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = !unitExpanded }) {
                OutlinedTextField(
                    value = unit.ifEmpty { "Unit" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                    isError = showValidationErrors && unit.isBlank()
                )
                if (showValidationErrors && unit.isBlank()) {
                    Text("Unit required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                    units.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                            unit = selection
                            unitExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(fieldSpacing))

            // District (admin & registration editable; self-edit disabled)
            val isSpecialUnit = remember(selectedUnitModel) {
                selectedUnitModel?.mappingType == "none"
            }

            if (!isHighRankingOfficer && !isSpecialUnit) {
                val isDistrictLocked = availableDistricts.size == 1 && district.isNotBlank()
                ExposedDropdownMenuBox(expanded = districtExpanded && !isDistrictLocked, onExpandedChange = {
                    if (!isSelfEdit && !isDistrictLocked) districtExpanded = !districtExpanded
                }) {
                    OutlinedTextField(
                        value = district.ifEmpty { if (isSelfEdit) district else "Select District / HQ*" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District / HQ*") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = !isDistrictLocked,
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
                if (showValidationErrors && district.isBlank()) Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(fieldSpacing))
            }


                // Manual Section (if "Others" is selected)
                if (station == "Others") {
                    OutlinedTextField(
                        value = manualSection,
                        onValueChange = { manualSection = it },
                        label = { Text("Specify ${if (!useStationLabel) "Section" else "Station"} Name*") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showValidationErrors && manualSection.isBlank()
                    )
                    if (showValidationErrors && manualSection.isBlank()) {
                        Text("Please specify name", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(fieldSpacing))
                }



            Spacer(Modifier.height(fieldSpacing))

            // Blood group (Hide for Officer, Hide if configured hidden)
            if (!isOfficer && isFieldVisible("bloodGroup")) {
                ExposedDropdownMenuBox(expanded = bloodGroupExpanded, onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded }) {
                    OutlinedTextField(value = bloodGroup.ifEmpty { "Select Blood Group" }, onValueChange = {}, readOnly = true, label = { Text("Blood Group") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) })
                    ExposedDropdownMenu(expanded = bloodGroupExpanded, onDismissRequest = { bloodGroupExpanded = false }) {
                        bloodGroups.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                bloodGroup = selection
                                bloodGroupExpanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.height(fieldSpacing))
            }

            // KCSR Fields: Gender
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) }
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
                Spacer(Modifier.height(fieldSpacing))
            }

            // KCSR Fields: Date of Birth and Date of Appointment
            if (isFieldVisible("dob")) {
                val dobCalendar = Calendar.getInstance()
                dateOfBirth?.let { dobCalendar.time = it }

                val dobPickerDialog = DatePickerDialog(
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

                OutlinedTextField(
                    value = dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Date of Birth") },
                    modifier = Modifier.fillMaxWidth().clickable { dobPickerDialog.show() },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(fieldSpacing))
            }

            if (isFieldVisible("doa")) {
                val apptCalendar = Calendar.getInstance()
                serviceStartDate?.let { apptCalendar.time = it }

                val apptPickerDialog = DatePickerDialog(
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

                OutlinedTextField(
                    value = serviceStartDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                    onValueChange = {},
                    label = { Text("Date of Appointment") },
                    modifier = Modifier.fillMaxWidth().clickable { apptPickerDialog.show() },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(Modifier.height(fieldSpacing))
            }
        }

        // Submit
        Button(
            onClick = {
                
                // ✅ Prevent duplicate submissions
                if (isSubmitting || isLoading) {
                    android.util.Log.d("CommonEmployeeForm", "⚠️ Already submitting, returning early")
                    Toast.makeText(context, "Please wait, submission in progress...", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                android.util.Log.d("CommonEmployeeForm", "✅ Starting validation...")
                showValidationErrors = true
                
                // Basic validation
                // Officer email is optional, Employee email is required
                val isEmailValid = if (isOfficer && email.isNullOrBlank()) true else isValidEmail(email ?: "")
                if (!isEmailValid || !isValidMobile(mobile1) || name.isBlank()) {
                    Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                // KGID validation (required for admin add and registration, optional for self-edit)
                if (!isSelfEdit && kgid.isBlank()) {
                    Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                // Registration-specific validation
                if (isRegistration) {
                    // Validate rank
                    if (rank.isBlank()) {
                        Toast.makeText(context, "Rank is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate metal number if required for selected rank
                    if (showMetalNumberField && metalNumber.isBlank()) {
                        Toast.makeText(context, "Metal number is required for this rank", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate district
                    if (district.isBlank() && !isHighRankingOfficer) {
                        Toast.makeText(context, "District is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate station
                    if (station.isBlank() && !isMinisterial && !isHighRankingOfficer) {
                        Toast.makeText(context, "Station required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Validate Manual Section
                    if (station == "Others" && manualSection.isBlank()) {
                        Toast.makeText(context, "Please specify your section name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate PIN
                    if (pin.length != 6 || pin != confirmPin) {
                        Toast.makeText(context, "PIN mismatch or invalid", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Validate terms acceptance
                    if (!acceptedTerms) {
                        Toast.makeText(context, "Accept terms to continue", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                }

                // ✅ Set submitting state to prevent duplicate clicks
                android.util.Log.d("CommonEmployeeForm", "✅ Validation passed, setting isSubmitting = true")
                isSubmitting = true

                val finalKgid = if (kgid.isBlank()) "TEMP-${System.currentTimeMillis()}" else kgid
                android.util.Log.d("CommonEmployeeForm", "📝 Final KGID: $finalKgid")

                val isManual = station == "Others" // Determine if manual station

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
                    station = if (isManual) manualSection.trim() else station.trim(),
                    unit = unit.trim().takeIf { it.isNotBlank() },
                    bloodGroup = bloodGroup.ifBlank { null },
                    metalNumber = metalNumber.trim().takeIf { it.isNotBlank() },
                    isAdmin = initialEmployee?.isAdmin ?: false,
                    photoUrl = croppedPhotoUri?.toString() ?: currentPhotoUrl,
                    isManualStation = isManual,
                    gender = gender,
                    serviceStartDate = serviceStartDate,
                    dateOfBirth = dateOfBirth
                )

                // ✅ Submit in coroutine scope
                android.util.Log.d("CommonEmployeeForm", "🚀 Launching coroutine for submission...")
                coroutineScope.launch {
                    try {
                        android.util.Log.d("CommonEmployeeForm", "📋 Inside coroutine, isRegistration: $isRegistration")
                        if (isRegistration) {
                            // Build PendingRegistrationEntity and call callback
                            val firebaseUid = "" // wrapper will add actual uid
                            val pending = PendingRegistrationEntity(
                                name = emp.name,
                                kgid = emp.kgid,
                                email = emp.email,
                                mobile1 = emp.mobile1 ?: "",
                                mobile2 = emp.mobile2,
                                pin = pin,
                                rank = emp.rank ?: "",
                                metalNumber = emp.metalNumber,
                                district = emp.district.orEmpty(),
                                station = if (station == "Others") manualSection.trim() else emp.station.orEmpty(),
                                unit = emp.unit,
                                bloodGroup = emp.bloodGroup.orEmpty(),
                                firebaseUid = firebaseUid,
                                photoUrl = emp.photoUrl,
                                isManualStation = emp.isManualStation,
                                gender = emp.gender,
                                serviceStartDate = emp.serviceStartDate
                            )
                            onRegisterSubmit?.invoke(pending, croppedPhotoUri)
                        } else {
                            android.util.Log.d("CommonEmployeeForm", "═══════════════════════════════════════")
                            android.util.Log.d("CommonEmployeeForm", "📤 Calling onSubmit with photo: ${croppedPhotoUri != null}")
                            android.util.Log.d("CommonEmployeeForm", "📤 Employee KGID: ${emp.kgid}")
                            onSubmit(emp, croppedPhotoUri)
                            android.util.Log.d("CommonEmployeeForm", "✅ onSubmit call completed")
                        }
                        // Reset after 3 seconds (allowing time for operation)
                        delay(3000)
                        isSubmitting = false
                    } catch (e: Exception) {
                        android.util.Log.e("CommonEmployeeForm", "❌ Submission error: ${e.message}", e)
                        e.printStackTrace()
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting && !isLoading, // ✅ Disable button during submission
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting || isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Submitting...")
                }
            } else {
                Text(
                    when {
                        isRegistration -> "Submit for approval"
                        initialEmployee != null -> "Submit update for approval"
                        else -> "Submit for approval"
                    }
                )
            }
        }

        if (isSubmitting || isLoading) {
            val statusTitle = when {
                isLoading -> "Uploading details to server..."
                else -> "Preparing submission..."
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload in progress",
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

        Spacer(Modifier.height(sectionSpacing))

        // Image chooser
        if (showSourceDialog) {
            AlertDialog(onDismissRequest = { showSourceDialog = false }, title = { Text("Select Image Source") }, text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showSourceDialog = false
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(Modifier.width(12.dp))
                        Text("Camera")
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(Modifier.width(12.dp))
                        Text("Gallery")
                    }
                }
            }, confirmButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Close") }
            })
        }
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
            setStatusBarColor(androidx.core.content.ContextCompat.getColor(context, com.example.policemobiledirectory.R.color.md_theme_light_onPrimaryContainer))
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

