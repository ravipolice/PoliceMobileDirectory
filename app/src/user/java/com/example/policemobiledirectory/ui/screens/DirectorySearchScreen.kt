@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.style.TextAlign
import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.ui.theme.ErrorRed
import com.example.policemobiledirectory.ui.theme.CardShadow
import com.example.policemobiledirectory.ui.theme.BorderTeal
import com.example.policemobiledirectory.ui.theme.ChipSelectedStart
import com.example.policemobiledirectory.ui.theme.ChipSelectedEnd
import com.example.policemobiledirectory.ui.theme.ChipUnselected
import com.example.policemobiledirectory.ui.theme.BorderChipUnselected
import com.example.policemobiledirectory.ui.theme.GlassOpacity
import com.example.policemobiledirectory.ui.theme.PrimaryTeal
import com.example.policemobiledirectory.ui.theme.PrimaryTealDark
import com.example.policemobiledirectory.ui.theme.FABColor
import com.example.policemobiledirectory.ui.theme.BorderTeal
import com.example.policemobiledirectory.ui.theme.BackgroundLight
import com.example.policemobiledirectory.ui.theme.SecondaryYellow
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.viewmodel.EmployeeListViewModel
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.example.policemobiledirectory.viewmodel.SettingsViewModel

import com.example.policemobiledirectory.utils.OperationStatus
import kotlinx.coroutines.launch

import com.example.policemobiledirectory.ui.components.ContactCard
import com.example.policemobiledirectory.ui.components.SearchFilterBar
import kotlinx.coroutines.CoroutineScope
import com.example.policemobiledirectory.navigation.Routes
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectorySearchScreen(
    navController: NavController,
    viewModel: com.example.policemobiledirectory.viewmodel.EmployeeListViewModel,
    authViewModel: com.example.policemobiledirectory.viewmodel.AuthViewModel,
    settingsViewModel: com.example.policemobiledirectory.viewmodel.SettingsViewModel,
    constantsViewModel: com.example.policemobiledirectory.viewmodel.ConstantsViewModel = hiltViewModel(),
    notificationsViewModel: com.example.policemobiledirectory.viewmodel.NotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val isAdmin by authViewModel.isAdmin.collectAsStateWithLifecycle()
    val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    
    // Notification counts
    val notifications by notificationsViewModel.notifications.collectAsStateWithLifecycle()
    val notificationCount = notifications.count { !it.isRead }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) { 
        // Only refresh if we're on the directory search screen
        if (currentRoute == Routes.DIRECTORY_SEARCH) {
            authViewModel.checkIfAdmin()
            authViewModel.loadSession()
            viewModel.setIsAdmin(authViewModel.isAdmin.value)
            // Refresh data when screen comes back into focus
            viewModel.refreshEmployees()
            viewModel.refreshOfficers()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text("Search Directory")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                    navigationIconContentColor = androidx.compose.ui.graphics.Color.White,
                    actionIconContentColor = androidx.compose.ui.graphics.Color.White
                ),
                actions = {
                    IconButton(onClick = { shareAppLink(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share App")
                    }
                    IconButton(onClick = { 
                        viewModel.refreshEmployees()
                        viewModel.refreshOfficers()
                        constantsViewModel.forceRefresh()  // ← Also refresh constants from Google Sheet
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    
                    // Single Font Size Button with Dropdown Menu
                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale ->
                            settingsViewModel.setFontScale(scale)
                        },
                        onFontScaleToggle = {
                            // Cycle through common presets: 0.8, 1.0, 1.2, 1.4, 1.6, 1.8
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { 
                                kotlin.math.abs(it - current) < 0.05f 
                            }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) {
                                currentIndex + 1
                            } else {
                                0 // Cycle back to first
                            }
                            settingsViewModel.setFontScale(presets[nextIndex])
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    IconButton(onClick = { settingsViewModel.toggleTheme() }) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {}
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background // Themed background
        ) {
            EmployeeListContent(
                navController = navController,
                viewModel = viewModel,
                authViewModel = authViewModel,
                constantsViewModel = constantsViewModel,
                context = context,
                isAdmin = isAdmin,
                fontScale = fontScale,
                snackbarHostState = snackbarHostState,

            )
        }
    }
}

@Composable
private fun EmployeeListContent(
    navController: NavController,
    viewModel: com.example.policemobiledirectory.viewmodel.EmployeeListViewModel,
    authViewModel: com.example.policemobiledirectory.viewmodel.AuthViewModel,
    constantsViewModel: com.example.policemobiledirectory.viewmodel.ConstantsViewModel,
    context: Context,
    isAdmin: Boolean,
    fontScale: Float,
    snackbarHostState: SnackbarHostState,

) {
    val coroutineScope = rememberCoroutineScope()
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val filteredContacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val officerStatus by viewModel.officerStatus.collectAsStateWithLifecycle()
    val searchFilter by viewModel.searchFilter.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    // Get constants from ViewModel
    val districts by constantsViewModel.districts.collectAsStateWithLifecycle()
    val units by constantsViewModel.units.collectAsStateWithLifecycle()
    val fullUnits by constantsViewModel.fullUnits.collectAsStateWithLifecycle()
    val stationsByDistrict by constantsViewModel.stationsByDistrict.collectAsStateWithLifecycle()
    val ranks by constantsViewModel.ranks.collectAsStateWithLifecycle()
    val districtShortCodeMap by constantsViewModel.districtShortCodeMap.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    
    // 🔹 FILTER STATES FROM VIEWMODEL
    val selectedUnit by viewModel.selectedUnit.collectAsStateWithLifecycle()
    val selectedDistrict by viewModel.selectedDistrict.collectAsStateWithLifecycle()
    val selectedStation by viewModel.selectedStation.collectAsStateWithLifecycle()
    val selectedRank by viewModel.selectedRank.collectAsStateWithLifecycle()

    // Check if selected unit is District Level (hides Station)
    val isDistrictLevelUnit by produceState(initialValue = false, key1 = selectedUnit) {
        value = constantsViewModel.isDistrictLevelUnit(selectedUnit)
    }

    // Filter Units based on selected District
    val filteredUnitNames = remember(fullUnits, units, selectedDistrict) {
        if (selectedDistrict == "All" || selectedDistrict.isBlank()) {
             units
        } else {
             if (fullUnits.isNotEmpty()) {
                 fullUnits.filter { u ->
                     when (u.mappingType) {
                         "all", "" -> true
                         "none" -> true
                         "state" -> selectedDistrict == "HQ"
                         "commissionerate", "single", "subset" -> u.mappedDistricts.any { it.equals(selectedDistrict, ignoreCase = true) }
                         else -> true 
                     }
                 }.map { it.name }.sorted()
             } else {
                 units
             }
        }
    }

    // 🔹 DYNAMIC DISTRICTS LIST (Sync with Admin Mapping)
    val districtsList = remember(isAdmin, selectedUnit, districts) {
        val baseList = if (selectedUnit == "All") districts else constantsViewModel.getDistrictsForUnit(selectedUnit)
        listOf("All") + baseList // Allow 'All' for everyone
    }

    // 🔹 DYNAMIC DROPDOWNS & LABELS
    val stationsForDistrict by produceState<List<String>>(initialValue = listOf("All"), key1 = selectedUnit, key2 = selectedDistrict) {
        val resolved = constantsViewModel.getStationsAndSectionsForUnit(selectedUnit, selectedDistrict)
        value = listOf("All") + resolved
    }
    val allRanks = remember(ranks) { listOf("All") + ranks }
    val allUnitNames = remember(filteredUnitNames) { if (filteredUnitNames.contains("All")) filteredUnitNames else listOf("All") + filteredUnitNames }


    // Removed auto-filter: all users (including regular users) now default to seeing "All" districts

    // val searchFields removed

    val listState = rememberLazyListState()

    // 🔹 Profile Verification Prompt Check
    val isProfileOutdated = remember(currentUser) {
        val lastUpdate = currentUser?.updatedAt
        if (lastUpdate == null) {
            false // Don't block new users, they will verify later
        } else {
            val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
            val diff = System.currentTimeMillis() - lastUpdate.time
            diff > ninetyDaysInMillis
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Themed background
    ) {

        // 🔹 DYNAMIC LABELS
        val unitObj = fullUnits.find { it.name == selectedUnit }
        val districtLabel = if (unitObj?.mappedAreaType == "BATTALION") "Battalion" else "District / HQ"
        val hasDistrictStationsScope = unitObj?.scopes?.contains("district_stations") == true
        val stationLabel = if (stationsForDistrict.size > 1 && 
            !stationsForDistrict.contains("Others") && 
            selectedUnit != "All" && 
            selectedUnit != "Law & Order" && 
            selectedUnit != "L&O" && 
            !hasDistrictStationsScope
        ) "Section" else "Station / Section"

        if (isProfileOutdated) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile Verification Required",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Please verify your station and designation to keep the directory updated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = { navController.navigate(Routes.MY_PROFILE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Review", fontSize = 12.sp)
                    }
                }
            }
        }

        val aiSearchStatus by viewModel.aiSearchStatus.collectAsStateWithLifecycle()

        // 🔹 3. Search Bar (Integrated below categories)
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            SearchFilterBar(
                units = allUnitNames,
                districts = districtsList,
                stations = stationsForDistrict,
                ranks = allRanks,
                selectedUnit = selectedUnit,
                selectedDistrict = selectedDistrict,
                selectedStation = selectedStation,
                selectedRank = selectedRank,
                onUnitChange = { viewModel.updateSelectedUnit(it) },
                onDistrictChange = { district ->
                    viewModel.updateSelectedDistrict(district)
                    viewModel.updateSelectedStation("All")
                },
                onStationChange = { viewModel.updateSelectedStation(it) },
                onRankChange = { viewModel.updateSelectedRank(it) },
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onAISearch = { viewModel.performAISearch(it) },
                aiStatus = aiSearchStatus,
                isDistrictLevelUnit = isDistrictLevelUnit,
                isAdmin = isAdmin,
                districtLabel = districtLabel,
                stationLabel = stationLabel,
                totalContactsCount = allContacts.size,
                filteredContactsCount = filteredContacts.size,
                modifier = Modifier
            )
        }




        // 🔹 UNIFIED CONTACTS LIST (Employees + Officers)
        Box(modifier = Modifier.weight(1f)) {
            when {
                // Show loading for Idle state too (initial launch) to prevent "No contacts" flash
                employeeStatus is OperationStatus.Loading || officerStatus is OperationStatus.Loading || 
                employeeStatus is OperationStatus.Idle || officerStatus is OperationStatus.Idle -> {
                     Box(Modifier.fillMaxSize(), Alignment.Center) {
                         CircularProgressIndicator(color = PrimaryTeal)
                     }
                }
                employeeStatus is OperationStatus.Error || officerStatus is OperationStatus.Error -> {
                    val errorMessage = (employeeStatus as? OperationStatus.Error)?.message 
                        ?: (officerStatus as? OperationStatus.Error)?.message 
                        ?: "Something went wrong"
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Could not load contacts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.refreshEmployees()
                                viewModel.refreshOfficers()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                filteredContacts.isEmpty() -> {
                    val isFiltered = searchQuery.isNotEmpty() || 
                                    selectedDistrict != "All" || 
                                    selectedUnit != "All" || 
                                    selectedStation != "All" || 
                                    selectedRank != "All"
                    
                    val isUnapproved = currentUser?.isApproved != true && !isAdmin

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Column(
                             horizontalAlignment = Alignment.CenterHorizontally,
                             verticalArrangement = Arrangement.spacedBy(8.dp),
                             modifier = Modifier.padding(32.dp)
                         ) {
                             if (isUnapproved) {
                                 Icon(
                                     imageVector = Icons.Default.Lock,
                                     contentDescription = null,
                                     modifier = Modifier.size(48.dp),
                                     tint = Color.Gray.copy(alpha = 0.5f)
                                 )
                                 Text(
                                     text = "Access Restricted",
                                     style = MaterialTheme.typography.titleLarge,
                                     fontWeight = FontWeight.Bold,
                                     color = Color.Gray
                                 )
                                 Text(
                                     text = "Your account is pending approval. You will see contacts once an administrator approves your access.",
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = Color.Gray,
                                     textAlign = TextAlign.Center
                                 )
                             } else {
                                 Icon(
                                     imageVector = Icons.Default.SearchOff,
                                     contentDescription = null,
                                     modifier = Modifier.size(48.dp),
                                     tint = Color.Gray.copy(alpha = 0.5f)
                                 )
                                 Text(
                                     text = if (isFiltered) "No contacts match your filters." else "No contacts found in directory.",
                                     style = MaterialTheme.typography.bodyLarge,
                                     color = Color.Gray,
                                     textAlign = TextAlign.Center
                                 )
                                 
                                 if (isFiltered) {
                                     Button(
                                         onClick = {
                                             viewModel.updateSelectedUnit("All")
                                             viewModel.updateSelectedDistrict("All")
                                             viewModel.updateSelectedStation("All")
                                             viewModel.updateSelectedRank("All")
                                             viewModel.updateSearchQuery("")
                                         },
                                         colors = ButtonDefaults.buttonColors(
                                             containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                             contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                         )
                                     ) {
                                         Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(18.dp))
                                         Spacer(Modifier.width(8.dp))
                                         Text("Reset All Filters")
                                     }
                                 }
                             }
                         }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            items = filteredContacts,
                            key = { it.id },
                            contentType = { "contact" }
                        ) { contact ->
                            Column {
                            ContactCard(
                                    employee = contact.employee,
                                    officer = contact.officer,
                                    fontScale = fontScale,
                                    isAdmin = false,
                                    onEdit = null,
                                    onClick = {
                                        val id = contact.employee?.kgid ?: contact.officer?.agid ?: ""
                                        val isOfficer = contact.officer != null
                                        if (id.isNotEmpty()) {
                                            navController.navigate(Routes.employeeDetailRoute(id, isOfficer))
                                        }
                                    }
                                )
                                // Kerala Style Divider
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color.LightGray.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }


        // 🔹 DELETE CONFIRMATION + SNACKBAR (Improved with Auto Refresh)


    }
}

/**
 * Single Font Size Selector Button
 * - Click to cycle through common preset sizes (0.8, 1.0, 1.2, 1.4, 1.6, 1.8)
 * - Long press opens menu with all size options for precise selection
 */
@Composable
private fun FontSizeSelectorButton(
    currentFontScale: Float,
    onFontScaleSelected: (Float) -> Unit,
    onFontScaleToggle: () -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val presetSizes = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f)
    
    Box {
        // Single button with combined clickable for click and long press
        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = onFontScaleToggle,
                    onLongClick = { showDropdownMenu = true }
                )
                .padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${(currentFontScale * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Font Size (Click to cycle, Long press for menu)",
                    modifier = Modifier.size(18.dp),
                    tint = contentColor.copy(alpha = 0.7f)
                )
            }
        }
        
        // Dropdown Menu
        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
        ) {
            presetSizes.forEach { size ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${(size * 100).toInt()}%",
                            fontWeight = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    onClick = {
                        onFontScaleSelected(size)
                        showDropdownMenu = false
                    },
                    leadingIcon = if (kotlin.math.abs(size - currentFontScale) < 0.05f) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Current size",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}
