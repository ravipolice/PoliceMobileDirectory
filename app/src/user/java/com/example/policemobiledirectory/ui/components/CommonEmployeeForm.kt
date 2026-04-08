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
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
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
    initialEmail: String = "", // âœ… Add initialEmail parameter for prefilling
    initialName: String = "",
    onSubmit: (Employee, Uri?) -> Unit,
    onRegisterSubmit: ((PendingRegistrationEntity, Uri?) -> Unit)? = null,
    isLoading: Boolean = false, // âœ… Add loading state parameter
    onNavigateToTerms: (() -> Unit)? = null, // âœ… Callback to navigate to terms
    constantsViewModel: ConstantsViewModel = hiltViewModel(),
    isOfficer: Boolean = false, // âœ… New parameter for Officer mode
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) } // âœ… Track submission state

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
    val fullUnits by constantsViewModel.fullUnits.collectAsStateWithLifecycle() // âœ… Need full objects for hidden flag
    val globalHiddenFields by constantsViewModel.globalHiddenFields.collectAsStateWithLifecycle() // âœ… Global Hidden Fields
    val ranksWithAutoAgid by constantsViewModel.ranksWithAutoAgid.collectAsStateWithLifecycle() // âœ… Ranks that auto-generate AGID

    val ksrpBattalions by constantsViewModel.ksrpBattalions.collectAsStateWithLifecycle()


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
    // âœ… Use initialEmail if provided, otherwise use initialEmployee.email
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
    var dateOfBirth by remember(initialEmployee) { mutableStateOf<Date?>(initialEmployee?.dateOfBirth) } // âœ… New DOB field
    var genderExpanded by remember { mutableStateOf(false) }

    // registration extras
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }

    val selectedUnitModel = remember(unit, fullUnits) {
        fullUnits.find { it.name == unit }
    }

    val isSpecialUnit = remember(selectedUnitModel) {
        selectedUnitModel?.mappingType == "none"
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

    // Check if rank auto-generates AGID (Hides the field)
    val isAutoAgid = remember(rank, ranksWithAutoAgid) {
        ranksWithAutoAgid.contains(rank)
    }

    // Dynamic District List Logic (Hybrid Strategy)
    // Fetches from Firestore Cache -> Fallback to Hardcoded
    val availableDistricts by produceState<List<String>>(initialValue = emptyList(), key1 = unit) {
        // Run in IO context to avoid UI thread blocking if it hits disk/db
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            constantsViewModel.getDistrictsForUnit(unit)
        }
    }
    
    // Dynamic Unit Sections
    val unitSections by produceState<List<String>>(initialValue = emptyList(), key1 = unit) {
        if (unit.isNotBlank()) {
            value = constantsViewModel.getSectionsForUnit(unit)
        } else {
            value = emptyList()
        }
    }

    // Identify if it has custom sections (e.g. State INT or manual sections)
    val hasSections = remember(unitSections, unit, district) {
        unitSections.isNotEmpty() || unit == "State INT" || district == "HQ"
    }

    // Check if selected unit is "District Level" (No Station)
    val isDistrictLevelUnit by produceState(initialValue = false, key1 = unit) {
        value = constantsViewModel.isDistrictLevelUnit(unit)
    }

    // Dynamic Label Logic (Depends on Unit Configuration)
    val stationLabel = remember(unit, isDistrictLevelUnit, selectedUnitModel) {
        val keyword = selectedUnitModel?.stationKeyword
        if (!keyword.isNullOrBlank()) {
            keyword
        } else if (unit == "Law & Order" || !isDistrictLevelUnit) {
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

    // Auto-select if only one district is available, or validate current selection
    LaunchedEffect(availableDistricts) {
        if (availableDistricts.isNotEmpty()) {
            if (availableDistricts.size == 1) {
                val autoSelected = availableDistricts.first()
                if (district != autoSelected) {
                    district = autoSelected
                }
            } else {
                 // If the currently selected district is not in the new allowed list, reset it
                 if (district.isNotBlank() && !availableDistricts.contains(district)) {
                     district = ""
                     station = ""
                 }
            }
        }
    }



    // Validate Station Selection when options change (e.g. Unit change filters stations)
    // We can't rely solely on district change because Unit A and Unit B might share a district
    // but have different station subsets (e.g. Traffic vs Civil).


    // Reset manual sub-section if duty role selection changes away from "Others"
    LaunchedEffect(subSection) {
        if (subSection != "Others") {
            manualSubSection = ""
        }
    }

    // Reset station when district changes (Manual override or cascade)
    var previousDistrict by remember(initialEmployee) { mutableStateOf(district) }
    LaunchedEffect(district) {
        if (district != previousDistrict) {
            if (district.isNotBlank() && station.isNotBlank()) {
                 // Only reset if strict validation fails? 
                 // Actually, usually district change implies station reset.
                 // But we have the validation above. 
                 // Let's keep strict reset on district change to be safe and predictable.
                 station = ""
            }
            previousDistrict = district
        }
    }

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

    // Optimization: Create a normalized, case-insensitive map for fast lookups
    val normalizedStationsMap = remember(stationsByDistrict) {
        stationsByDistrict.entries.associate { (key, value) ->
            key.trim().lowercase() to value
        }
    }

    // 🔎 CENTRALIZED STATIONS & SECTIONS LOGIC
    val stationsForSelectedDistrict by produceState<List<String>>(initialValue = emptyList(), key1 = unit, key2 = district) {
        value = constantsViewModel.getStationsAndSectionsForUnit(unit, district)
    }

    // Identify if label should be "Station" or "Section"
    // DEPRECATED in favor of dynamic stationLabel above
    val useStationLabel = remember(unit, isDistrictLevelUnit) {
        unit == "Law & Order" || !isDistrictLevelUnit
    }

    // Validate Station Selection when options change (e.g. Unit change filters stations)
    LaunchedEffect(stationsForSelectedDistrict) {
        if (stationsForSelectedDistrict.isNotEmpty() && station.isNotBlank() && station != "Others" && !stationsForSelectedDistrict.contains(station)) {
             station = ""
        }
    }

    // UCrop launcher
    val uCropResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        when (result.resultCode) {
            android.app.Activity.RESULT_OK -> {
                UCrop.getOutput(result.data!!)?.let { croppedPhotoUri = it }
            }
            UCrop.RESULT_ERROR -> {
                val err = UCrop.getError(result.data!!)
                Toast.makeText(context, "Crop error: ${err?.message}", Toast.LENGTH_SHORT).show()
            }
            android.app.Activity.RESULT_CANCELED -> {
                Toast.makeText(context, "Image selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // camera preview -> cache -> uCrop
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        bmp?.let {
            val src = saveBitmapToCacheAndGetUri(context, it)
            if (src != null) launchUCrop(context, src, uCropResultLauncher)
            else Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    // camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
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
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
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
                             tint = Color.White,
                             modifier = Modifier
                                 .align(Alignment.Center)
                                 .background(Color.Black.copy(alpha = 0.4f), CircleShape)
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

            // Row 3: Email — pre-filled from login/signup, read-only
            OutlinedTextField(
                value = email,
                onValueChange = {},
                readOnly = true,
                label = { Text("Email*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
            Spacer(Modifier.height(fieldSpacing))

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





            Spacer(Modifier.height(fieldSpacing))

            // Row 6: Unit + District (FIRST — so rank can be filtered by unit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Unit
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(0.35f)
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
                    ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
                        filteredUnits.forEach { selection ->
                            DropdownMenuItem(text = { Text(selection) }, onClick = {
                                unit = selection
                                unitExpanded = false
                            })
                        }
                    }
                }
                if (showValidationErrors && unit.isBlank()) {
                     Text("Unit is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (!isHighRankingOfficer && !isSpecialUnit) {
                    val isDistrictLocked = availableDistricts.size == 1 && district.isNotBlank()
                    ExposedDropdownMenuBox(
                        expanded = districtExpanded && !isDistrictLocked,
                        onExpandedChange = {
                            if (!isSelfEdit && !isDistrictLocked) districtExpanded = !districtExpanded
                        },
                        modifier = Modifier.weight(0.65f)
                    ) {
                        OutlinedTextField(
                            value = district.ifEmpty { if (isSelfEdit) district else "Select District" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("District*") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            enabled = !isDistrictLocked,
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
            if (showValidationErrors && district.isBlank() && !isHighRankingOfficer && !isSpecialUnit) {
                Text("District required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 7: KGID + Rank (Rank is now filtered by the unit selected above)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KGID / ID (Hide if Auto-Generated OR Hidden by Admin)
                if (!isAutoAgid && isFieldVisible("kgid")) {
                    OutlinedTextField(
                        value = kgid,
                        onValueChange = { newValue ->
                            kgid = when {
                                // Allow anything for high-ranking officers
                                isOfficer || isHighRankingOfficer -> newValue
                                // Otherwise, only allow digits
                                newValue.all { it.isDigit() } -> newValue
                                // If new input contains non-digits, keep the old value
                                else -> kgid
                            }
                        },
                        label = { Text(identifierLabel) },
                        modifier = Modifier.weight(0.7f),
                        keyboardOptions = KeyboardOptions(keyboardType = if (isOfficer || isHighRankingOfficer) KeyboardType.Text else KeyboardType.Number),
                        isError = showValidationErrors && !isKgidValid(kgid),
                        enabled = (isAdmin || isRegistration) && !isSelfEdit
                    )
                }

                // Rank (filtered by selected unit)
                ExposedDropdownMenuBox(
                    expanded = rankExpanded,
                    onExpandedChange = { rankExpanded = !rankExpanded },
                    modifier = Modifier.weight(1.3f)
                ) {
                    OutlinedTextField(
                        value = rank,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rank*") },
                        placeholder = { Text(if (unit.isBlank()) "Select Unit First" else "Select Rank") },
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
            }
            if (showValidationErrors && rank.isBlank()) {
                Text(
                    "Rank is required",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            // Metal Number (conditional - only show when required AND NOT OFFICER AND VISIBLE)
            if (showMetalNumberField && !isOfficer && isFieldVisible("metalNumber")) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = metalNumber,
                    onValueChange = { metalNumber = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Metal No") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && metalNumber.isBlank()
                )
            }
            // Error messages below the row
            if (showValidationErrors) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (!isAutoAgid && !isKgidValid(kgid) && isFieldVisible("kgid")) {
                        Text(if(isOfficer) "ID required" else "KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    if (rank.isBlank()) {
                        Text("Rank required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    if (showMetalNumberField && metalNumber.isBlank() && !isOfficer && isFieldVisible("metalNumber")) {
                        Text("Metal no. required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(fieldSpacing))

            // Row 8: Station/Section (Full Width)
            // District-level units (e.g. DAR) always hide this field - hasSections must not override
            if (!isHighRankingOfficer && !isDistrictLevelUnit && !isSpecialUnit && !isMinisterial) {
                val filteredStations = remember(stationsForSelectedDistrict, rank, policeStationRanks) {
                    val isPoliceStationRank = policeStationRanks.any { it.equals(rank, ignoreCase = true) }
                    if (isPoliceStationRank) {
                        // Police Station Ranks see ONLY PS stations and Sections to reduce noise
                        // but they should also see specialized sections/branches
                        stationsForSelectedDistrict.filter { it.contains(" PS", ignoreCase = true) || !it.contains(" PS", ignoreCase = true) } 
                        // Actually, if it's a PS rank, they usually only look for their PS.
                        // But if sections are combined, we should show them too.
                        // Simplified: only filter if they are DEFINITELY stations.
                        stationsForSelectedDistrict.filter { 
                            !it.contains("PS", ignoreCase = true) || it.contains(" PS", ignoreCase = true) 
                        }
                        // Actually, let's stick to the previous logic but ensure it doesn't break combined results
                        stationsForSelectedDistrict.filter { s ->
                            val isPS = s.contains(" PS", ignoreCase = true)
                            // If it's a PS rank, they see the PS. If it's a branch, they see it too?
                            // Usually, PS ranks don't work in branches like "Accounts".
                            // But for L&O, they might.
                            if (isPoliceStationRank && s.contains(" PS", ignoreCase = true)) true
                            else if (!s.contains(" PS", ignoreCase = true)) true // Show regular sections/branches
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
                                if (district.isNotBlank() || stationsForSelectedDistrict.isNotEmpty()) "Select $stationLabel" 
                                else "Select District First" 
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stationLabel) },
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
                        if (station.isBlank()) {
                            Text(
                                "$stationLabel required", 
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





            Spacer(Modifier.height(fieldSpacing))

            // Duty Role and Blood Group Row - Registration
            val registrationDutyRoles = remember(unit, subSectionList, fullUnits) {
                constantsViewModel.getDutyRolesForUnit(unit, isRegistration)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duty Role Dropdown (Sub-Section) - Registration
                if (registrationDutyRoles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = subSectionExpanded,
                        onExpandedChange = { subSectionExpanded = !subSectionExpanded },
                        modifier = Modifier.weight(0.65f)
                    ) {
                        OutlinedTextField(
                            value = subSection.ifEmpty { "Select Duty Role" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Duty Role / Sub-Section") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subSectionExpanded) }
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

                // Blood Group row (Hide for Officer, Hide if configured hidden)
                if (!isOfficer && isFieldVisible("bloodGroup")) {
                    ExposedDropdownMenuBox(
                        expanded = bloodGroupExpanded,
                        onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                        modifier = Modifier.weight(0.35f)
                    ) {
                        OutlinedTextField(
                            value = bloodGroup.ifEmpty { "Blood Group" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                            isError = showValidationErrors && bloodGroup.isBlank(),
                            singleLine = true
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
            // Error messages
            if (showValidationErrors && bloodGroup.isBlank() && !isOfficer && isFieldVisible("bloodGroup")) {
                Text("Blood Group is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(fieldSpacing))

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

            // Date of Birth
             if (isFieldVisible("dob")) {
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

                 OutlinedTextField(
                     value = dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                     onValueChange = { },
                     label = { Text("Date of Birth") },
                     readOnly = true,
                     trailingIcon = {
                         Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { dobDatePicker.show() })
                     },
                     modifier = Modifier.fillMaxWidth().clickable { dobDatePicker.show() },
                     enabled = false, // Make text field disabled so click passes to modifier
                     colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 )
                Spacer(Modifier.height(fieldSpacing))
             }

             // Date of Appointment
             if (isFieldVisible("doa")) {
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

                 OutlinedTextField(
                     value = serviceStartDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                     onValueChange = { },
                     label = { Text("Date of Appointment") },
                     readOnly = true,
                     trailingIcon = {
                         Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { apptDatePicker.show() })
                     },
                     modifier = Modifier.fillMaxWidth().clickable { apptDatePicker.show() },
                     enabled = false,
                     colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 )
                Spacer(Modifier.height(sectionSpacing))
             }

            // Row 9: Create PIN, Confirm PIN (on same row) - Hide for Officer
            if (!isOfficer && isFieldVisible("pin")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = { Text("Create 6 Digit PIN*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = showValidationErrors && (pin.length != 6 || (pin.isNotEmpty() && pin != confirmPin)),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                        label = { Text("Confirm 6 Digit PIN*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = showValidationErrors && (confirmPin.length != 6 || (confirmPin.isNotEmpty() && pin != confirmPin)),
                        singleLine = true
                    )
                }
                if (showValidationErrors && (pin.length != 6 || pin != confirmPin)) {
                    Text("PIN must be 6 digits and match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(fieldSpacing))
            }
        } else {
            // Non-registration form (admin/self-edit) - keep original order
            // KGID (admin & registration only)
            if (!isSelfEdit && !isAutoAgid && isFieldVisible("kgid")) {
                OutlinedTextField(
                     value = kgid,
                     onValueChange = { newValue ->
                          // For Officers, allow any characters (AGID). Otherwise, only allow digits (KGID).
                          kgid = when {
                                isOfficer || isHighRankingOfficer -> newValue
                                newValue.all { ch -> ch.isDigit() } -> newValue
                                else -> kgid
                          }
                     },
                    label = { Text(identifierLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (isOfficer || isHighRankingOfficer) KeyboardType.Text else KeyboardType.Number),
                    isError = showValidationErrors && !isKgidValid(kgid),
                    enabled = isAdmin || isRegistration
                )
                if (showValidationErrors && !isKgidValid(kgid)) {
                    Text(if(isOfficer || isHighRankingOfficer) "ID required" else "KGID required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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

            // Unit (Moved here for proper dependency flow)
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
                    filteredUnits.forEach { selection ->
                        DropdownMenuItem(text = { Text(selection) }, onClick = {
                            unit = selection
                            unitExpanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(fieldSpacing))

                // District (admin & registration editable; self-edit disabled)
                if (!isHighRankingOfficer && !isSpecialUnit) {
                    val isDistrictLocked = availableDistricts.size == 1 && district.isNotBlank()
                    ExposedDropdownMenuBox(expanded = districtExpanded && !isDistrictLocked, onExpandedChange = {
                        if (!isSelfEdit && !isDistrictLocked) districtExpanded = !districtExpanded
                    }) {
                        OutlinedTextField(
                            value = district.ifEmpty { if (isSelfEdit) district else "Select District" },
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

                // Identify if it has custom sections (e.g. State INT or manual sections)
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
                            
                            // Also add "Others" if it's an HQ-level unit (where we might need manual names)
                            if (hasSections || district == "HQ") {
                                baseStations + listOf("Others")
                            } else {
                                baseStations
                            }
                        }
                    }

                    ExposedDropdownMenuBox(expanded = stationExpanded, onExpandedChange = {
                        if ((district.isNotBlank() || hasSections) && filteredStations.isNotEmpty()) stationExpanded = !stationExpanded
                    }) {
                        OutlinedTextField(
                            value = station.ifEmpty { 
                                if (hasSections) "Select Section" 
                                else if (district.isNotBlank()) "Select Station" 
                                else "Select District / HQ First" 
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stationLabel) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationExpanded) },
                            enabled = (district.isNotBlank() || hasSections) && filteredStations.isNotEmpty(),
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
                    if (showValidationErrors && station.isBlank()) Text(if (hasSections) "Section required" else "Station required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(fieldSpacing))

                    // Manual Section (if "Others" is selected)
                    if (station == "Others") {
                        OutlinedTextField(
                            value = manualSection,
                            onValueChange = { manualSection = it },
                            label = { Text("Specify ${if (hasSections) "Section" else "Station"} Name*") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = showValidationErrors && manualSection.isBlank()
                        )
                        if (showValidationErrors && manualSection.isBlank()) {
                            Text("Please specify name", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(fieldSpacing))
                    }
                }

                // Duty Role Dropdown (Sub-Section) - Admin/Self-Edit
                val otherDutyRoles = remember(unit, subSectionList, fullUnits) {
                    constantsViewModel.getDutyRolesForUnit(unit, isRegistration)
                }

                // Row for Duty Role and Blood Group
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Duty Role Dropdown (Sub-Section) - Admin/Self-Edit
                    if (otherDutyRoles.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = subSectionExpanded,
                            onExpandedChange = { subSectionExpanded = !subSectionExpanded },
                            modifier = Modifier.weight(0.65f)
                        ) {
                            OutlinedTextField(
                                value = subSection.ifEmpty { "Select Duty Role" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Duty Role / Sub-Section") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subSectionExpanded) }
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

                    // Blood Group row (Hide for Officer, Hide if configured hidden)
                    if (!isOfficer && isFieldVisible("bloodGroup")) {
                        ExposedDropdownMenuBox(
                            expanded = bloodGroupExpanded, 
                            onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded },
                            modifier = Modifier.weight(0.35f)
                        ) {
                            OutlinedTextField(
                                value = bloodGroup.ifEmpty { "Blood Group" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Blood Group*", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                                isError = showValidationErrors && bloodGroup.isBlank(),
                                singleLine = true
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

                if (showValidationErrors && bloodGroup.isBlank() && !isOfficer && isFieldVisible("bloodGroup")) {
                    Text("Blood Group is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Handle manual entry for Duty Role below the row if "Others" is selected
                if (subSection == "Others") {
                    Spacer(Modifier.height(fieldSpacing))
                    OutlinedTextField(
                        value = manualSubSection,
                        onValueChange = { manualSubSection = it },
                        label = { Text("Specify Duty Role*") },
                        placeholder = { Text("Duty Role Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = showValidationErrors && manualSubSection.isBlank(),
                        singleLine = true
                    )
                    if (showValidationErrors && manualSubSection.isBlank()) {
                        Text("Duty role name required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
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
                Spacer(Modifier.height(sectionSpacing))
            }

             // Date of Birth
             if (isFieldVisible("dob")) {
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

                 OutlinedTextField(
                     value = dateOfBirth?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                     onValueChange = { },
                     label = { Text("Date of Birth") },
                     readOnly = true,
                     trailingIcon = {
                         Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { dobDatePicker.show() })
                     },
                     modifier = Modifier.fillMaxWidth().clickable { dobDatePicker.show() },
                     enabled = false, // Make text field disabled so click passes to modifier
                     colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 )
                Spacer(Modifier.height(fieldSpacing))
             }

             // Date of Appointment
             if (isFieldVisible("doa")) {
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

                 OutlinedTextField(
                     value = serviceStartDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "",
                     onValueChange = { },
                     label = { Text("Date of Appointment") },
                     readOnly = true,
                     trailingIcon = {
                         Icon(Icons.Default.Edit, contentDescription = "Select Date", modifier = Modifier.clickable { apptDatePicker.show() })
                     },
                     modifier = Modifier.fillMaxWidth().clickable { apptDatePicker.show() },
                     enabled = false,
                     colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                 )
                Spacer(Modifier.height(sectionSpacing))
             }
        }

        // Terms and condition (for registration)
        if (isRegistration) {
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
        }

        // Submit
        Button(
            onClick = {
                // ✅ Prevent duplicate submissions (Immediate check)
                if (isSubmitting || isLoading) {
                    return@Button
                }
                
                // ✅ Set submitting state IMMEDIATELY
                isSubmitting = true
                
                showValidationErrors = true
                
                // Basic validation
                val isEmailValid = if (!isFieldVisible("email")) true else if (isOfficer && email.isNullOrBlank()) true else isValidEmail(email ?: "")
                if (!isEmailValid || !isValidMobile(mobile1) || name.isBlank()) {
                    Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                    isSubmitting = false // Reset
                    return@Button
                }
                
                // KGID validation
                if (!isSelfEdit && !isAutoAgid && kgid.isBlank() && isFieldVisible("kgid")) {
                    Toast.makeText(context, "Please fix validation errors", Toast.LENGTH_SHORT).show()
                    isSubmitting = false // Reset
                    return@Button
                }
                
                // General validation
                if (!isAdmin) {
                    if (rank.isBlank()) {
                        Toast.makeText(context, "Rank is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                    if (showMetalNumberField && metalNumber.isBlank() && !isOfficer && isFieldVisible("metalNumber")) {
                        Toast.makeText(context, "Metal number is required for this rank", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                    if (district.isBlank() && !isHighRankingOfficer) {
                        Toast.makeText(context, "District is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                    if (station.isBlank() && !isHighRankingOfficer && !isDistrictLevelUnit && !isMinisterial) {
                        Toast.makeText(context, "${if(unit == "State INT" || unitSections.isNotEmpty()) "Section" else "Station"} is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                    if (station == "Others" && manualSection.isBlank()) {
                        Toast.makeText(context, "Please specify your section name", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                    if (bloodGroup.isBlank() && !isOfficer && isFieldVisible("bloodGroup")) {
                        Toast.makeText(context, "Blood Group is required", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                }

                // Registration-specific validation
                if (isRegistration) {
                    if (!isOfficer && isFieldVisible("pin") && (pin.length != 6 || pin != confirmPin)) {
                        Toast.makeText(context, "PIN mismatch or invalid", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                    if (!acceptedTerms) {
                        Toast.makeText(context, "Accept terms to continue", Toast.LENGTH_SHORT).show()
                        isSubmitting = false // Reset
                        return@Button
                    }
                }

                // Validate Manual Duty Role
                if (subSection == "Others" && manualSubSection.isBlank()) {
                    Toast.makeText(context, "Please specify your duty role", Toast.LENGTH_SHORT).show()
                    isSubmitting = false // Reset
                    return@Button
                }


                val finalKgid = if (kgid.isBlank()) "TEMP-${System.currentTimeMillis()}" else kgid

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
                    station = when {
                        isManual -> manualSection.trim()
                        station.isNotBlank() -> station.trim()
                        isDistrictLevelUnit -> district.trim() // For DAR etc: use district as station
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
                    dateOfBirth = dateOfBirth, // ✅ Pass DOB
                    subSection = if (subSection == "Others") manualSubSection.trim() else subSection.trim().takeIf { it.isNotBlank() },
                    isManualSubSection = subSection == "Others",
                    isHidden = false // defaults
                )

                // ✅ Submit in coroutine scope
                android.util.Log.d("CommonEmployeeForm", "🚀 Launching coroutine for submission...")
                coroutineScope.launch {
                    try {
                        android.util.Log.d("CommonEmployeeForm", "📝 Inside coroutine, isRegistration: $isRegistration")
                        if (isRegistration) {
                            // Build PendingRegistrationEntity and call callback
                            val firebaseUid = "" // wrapper will add actual uid
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
                                firebaseUid = firebaseUid,
                                photoUrl = emp.photoUrl,
                                isManualStation = emp.isManualStation,
                                gender = emp.gender,
                                serviceStartDate = emp.serviceStartDate,
                                dateOfBirth = emp.dateOfBirth, // ✅ Include DOB in pending
                                subSection = emp.subSection,
                                isManualSubSection = emp.isManualSubSection
                            )
                            onRegisterSubmit?.invoke(pending, croppedPhotoUri)
                        } else {
                            android.util.Log.d("CommonEmployeeForm", "–––––––––––––––––––––––––––––––––––––––––––––––––")
                            android.util.Log.d("CommonEmployeeForm", "✉️ Calling onSubmit with photo: ${croppedPhotoUri != null}")
                            android.util.Log.d("CommonEmployeeForm", "✉️ Employee KGID: ${emp.kgid}")
                            onSubmit(emp, croppedPhotoUri)
                            android.util.Log.d("CommonEmployeeForm", "âœ… onSubmit call completed")
                        }
                        // Reset after 3 seconds (allowing time for operation)
                        delay(3000)
                        isSubmitting = false
                    } catch (e: Exception) {
                        android.util.Log.e("CommonEmployeeForm", "âŒ Submission error: ${e.message}", e)
                        e.printStackTrace()
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting && !isLoading, // âœ… Disable button during submission
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
                            cameraLauncher.launch(null)
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
        
        Spacer(Modifier.height(120.dp)) // âœ… Ensure button is not hidden by anything
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
            
            // Fix Color Overlap
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

private fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            android.util.Log.e("CommonEmployeeForm", "FileProvider error: ${e.message}", e)
            Toast.makeText(context, "Failed to access file provider", Toast.LENGTH_SHORT).show()
            null
        }
    } catch (e: IOException) {
        android.util.Log.e("CommonEmployeeForm", "Failed to save bitmap: ${e.message}", e)
        Toast.makeText(context, "Failed to save image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        null
    } catch (e: Exception) {
        android.util.Log.e("CommonEmployeeForm", "Unexpected error: ${e.message}", e)
        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        null
    }
}
