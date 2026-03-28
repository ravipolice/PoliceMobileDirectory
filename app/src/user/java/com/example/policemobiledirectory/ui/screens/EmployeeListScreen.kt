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

import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    constantsViewModel: ConstantsViewModel = hiltViewModel(),
    notificationsViewModel: com.example.policemobiledirectory.viewmodel.NotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsStateWithLifecycle()
    val employeeStatus by viewModel.employeeStatus.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    
    // Notification counts
    val notifications by notificationsViewModel.notifications.collectAsStateWithLifecycle()
    val notificationCount = notifications.count { !it.isRead }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) { 
        // Only refresh if we're on the employee list screen
        if (currentRoute == Routes.EMPLOYEE_LIST) {
            viewModel.checkIfAdmin()
            // Refresh current user to check for approval status changes
            viewModel.refreshCurrentUser()
            // Refresh data when screen comes back into focus
            viewModel.refreshEmployees()
            viewModel.refreshOfficers()
        }
    }

    // ✅ Ensure status bar matches the PMD Home top bar color on this screen
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = PrimaryTeal,
            darkIcons = false
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PMD Home")
                        Spacer(Modifier.width(6.dp))
                        Box {
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                            // Notification badge - circular with white border (Yellow theme)
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .offset(x = 12.dp, y = (-12).dp)
                                        .background(
                                            color = SecondaryYellow, // Yellow for notification badge
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.White,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
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
                            viewModel.setFontScale(scale)
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
                            viewModel.setFontScale(presets[nextIndex])
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                    
                    IconButton(onClick = onThemeToggle) {
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
    viewModel: EmployeeViewModel,
    constantsViewModel: ConstantsViewModel,
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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

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

    // Show "All" only for admins, regular users see only districts
    // 🔹 DYNAMIC DISTRICTS LIST (Sync with Admin Mapping)
    val districtsList = remember(isAdmin, selectedUnit, districts) {
        val baseList = if (selectedUnit == "All") districts else constantsViewModel.getDistrictsForUnit(selectedUnit)
        if (isAdmin) listOf("All") + baseList else baseList
    }

    // 🔹 DYNAMIC DROPDOWNS & LABELS
    val stationsForDistrict by produceState<List<String>>(initialValue = listOf("All"), key1 = selectedUnit, key2 = selectedDistrict) {
        val resolved = constantsViewModel.getStationsAndSectionsForUnit(selectedUnit, selectedDistrict)
        value = listOf("All") + resolved
    }
    val allRanks = remember(ranks) { listOf("All") + ranks }

    // Initialize district to user's registered district when currentUser loads (for non-admins)
    LaunchedEffect(currentUser, isAdmin, districts) {
        if (!isAdmin) {
            // Regular user: set to their registered district (if it exists in the list)
            val userDistrict = currentUser?.district?.takeIf { it.isNotBlank() }
            if (userDistrict != null && districts.contains(userDistrict)) {
                viewModel.updateSelectedDistrict(userDistrict)
            } else if (districts.isNotEmpty() && selectedDistrict == "All") {
                // Fallback: use first district if user has no district set
                districts.firstOrNull()?.let { viewModel.updateSelectedDistrict(it) }
            }
        }
    }

    // val searchFields removed

    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // Clean white background for the whole screen
    ) {

        // 🔹 DYNAMIC LABELS
        val unitObj = fullUnits.find { it.name == selectedUnit }
        val districtLabel = if (unitObj?.mappedAreaType == "BATTALION") "Battalion" else "District / HQ"
        val stationLabel = if (stationsForDistrict.size > 1 && !stationsForDistrict.contains("Others") && selectedUnit != "All" && selectedUnit != "Law & Order") "Section" else "Station / Section"

        // 🔹 3. Search Bar (Integrated below categories)
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
            SearchFilterBar(
                units = filteredUnitNames,
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
                isDistrictLevelUnit = isDistrictLevelUnit,
                isAdmin = isAdmin,
                districtLabel = districtLabel,
                stationLabel = stationLabel,
                totalContactsCount = allContacts.size,
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
                        contentPadding = PaddingValues(bottom = 80.dp)
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
