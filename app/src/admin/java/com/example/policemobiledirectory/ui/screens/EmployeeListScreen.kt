@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel

import com.example.policemobiledirectory.data.local.SearchFilter
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.theme.*
import com.example.policemobiledirectory.ui.components.ContactCard
import com.example.policemobiledirectory.ui.components.SearchFilterBar
import com.example.policemobiledirectory.ui.theme.components.EmployeeCardAdmin
import com.example.policemobiledirectory.utils.Constants
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.ui.theme.CardStyle
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: EmployeeViewModel,
    onThemeToggle: () -> Unit,
    constantsViewModel: ConstantsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val filteredEmployees by viewModel.filteredEmployees.collectAsState()
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Notification counts
    val userNotifications by viewModel.userNotifications.collectAsState()
    val adminNotifications by viewModel.adminNotifications.collectAsState()
    val userNotificationsSeenAt by viewModel.userNotificationsLastSeen.collectAsState()
    val adminNotificationsSeenAt by viewModel.adminNotificationsLastSeen.collectAsState()
    val pendingApprovalsCount by viewModel.pendingApprovalsTotalCount.collectAsState()
    
    val notificationCount = if (isAdmin) {
        adminNotifications.count { (it.timestamp ?: 0L) > adminNotificationsSeenAt } + pendingApprovalsCount
    } else {
        userNotifications.count { (it.timestamp ?: 0L) > userNotificationsSeenAt }
    }

    // Get the current back stack entry to detect when screen comes back into focus
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    LaunchedEffect(currentRoute) { 
        if (currentRoute == Routes.EMPLOYEE_LIST) {
            viewModel.checkIfAdmin()
            viewModel.refreshEmployees()
            viewModel.refreshOfficers()
            constantsViewModel.forceRefresh()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PMD Home", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Box {
                            IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                            }
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .offset(x = 12.dp, y = (-10).dp)
                                        .background(SecondaryYellow, CircleShape)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { shareAppLink(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share App")
                    }
                    IconButton(onClick = {
                        viewModel.refreshEmployees()
                        viewModel.refreshOfficers()
                        constantsViewModel.forceRefresh()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale -> viewModel.setFontScale(scale) },
                        onFontScaleToggle = {
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { kotlin.math.abs(it - current) < 0.05f }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) currentIndex + 1 else 0
                            viewModel.setFontScale(presets[nextIndex])
                        },
                        contentColor = Color.White
                    )

                    IconButton(onClick = onThemeToggle) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Toggle Theme")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { navController.navigate("${Routes.ADD_EMPLOYEE}?employeeId=") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        EmployeeListContent(
            navController = navController,
            viewModel = viewModel,
            constantsViewModel = constantsViewModel,
            context = context,
            isAdmin = isAdmin,
            fontScale = fontScale,
            modifier = Modifier.padding(innerPadding)
        )
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
    modifier: Modifier = Modifier
) {
    val filteredContacts by viewModel.filteredContacts.collectAsState()
    val employeeStatus by viewModel.employeeStatus.collectAsState()
    val officerStatus by viewModel.officerStatus.collectAsState()
    val searchParams by viewModel.searchParams.collectAsState()
    val fullUnits by constantsViewModel.fullUnits.collectAsState()
    val districts by constantsViewModel.districts.collectAsState()
    val units by constantsViewModel.units.collectAsState()
    val ranks by constantsViewModel.ranks.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()

    val isDistrictLevelUnit by produceState(initialValue = false, key1 = searchParams.unit) {
        value = constantsViewModel.isDistrictLevelUnit(searchParams.unit)
    }
    
    val districtsList = remember(districts, searchParams.unit) {
        val baseList = if (searchParams.unit == "All") districts else constantsViewModel.getDistrictsForUnit(searchParams.unit)
        listOf("All") + baseList
    }

    val stationsForDistrict by produceState<List<String>>(initialValue = listOf("All"), key1 = searchParams.unit, key2 = searchParams.district) {
        val resolved = constantsViewModel.getStationsAndSectionsForUnit(searchParams.unit, searchParams.district)
        value = listOf("All") + resolved
    }

    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {

        // 🔹 SEARCH & FILTER BAR
        val unitObj = fullUnits.find { it.name == searchParams.unit }
        val districtLabel = if (unitObj?.mappedAreaType == "BATTALION") "Battalion" else "District / HQ"
        val stationLabel = if (stationsForDistrict.size > 1 && !stationsForDistrict.contains("Others") && searchParams.unit != "All") "Section" else "Station / Section"

        SearchFilterBar(
            units = units,
            districts = districtsList,
            stations = stationsForDistrict,
            ranks = listOf("All") + ranks,
            selectedUnit = searchParams.unit,
            selectedDistrict = searchParams.district,
            selectedStation = searchParams.station,
            selectedRank = searchParams.rank,
            onUnitChange = { viewModel.updateSelectedUnit(it) },
            onDistrictChange = { 
                viewModel.updateSelectedDistrict(it)
                viewModel.updateSelectedStation("All")
            },
            onStationChange = { viewModel.updateSelectedStation(it) },
            onRankChange = { viewModel.updateSelectedRank(it) },
            searchQuery = searchParams.query,
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            searchFilter = searchParams.filter,
            onSearchFilterChange = { viewModel.updateSearchFilter(it) },
            isDistrictLevelUnit = isDistrictLevelUnit,
            isAdmin = isAdmin,
            districtLabel = districtLabel,
            stationLabel = stationLabel,
            totalContactsCount = allContacts.size,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        // 🔹 Results List
        Box(modifier = Modifier.weight(1f)) {
            when {
                employeeStatus is OperationStatus.Loading || officerStatus is OperationStatus.Loading -> {
                     Box(Modifier.fillMaxSize(), Alignment.Center) {
                         CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                     }
                }
                filteredContacts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Text("No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                             if (searchParams.query.isNotEmpty() || searchParams.unit != "All") {
                                 TextButton(onClick = { viewModel.clearFilters() }) {
                                     Text("Clear All Filters")
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
                        items(filteredContacts, key = { it.id }) { contact ->
                            if (isAdmin && contact.employee != null) {
                                EmployeeCardAdmin(
                                    employee = contact.employee,
                                    isAdmin = true,
                                    fontScale = fontScale,
                                    navController = navController,
                                    context = context
                                )
                            } else {
                                ContactCard(
                                    officer = contact.officer,
                                    fontScale = fontScale,
                                    isAdmin = isAdmin,
                                    onEdit = { navController.navigate("${Routes.ADD_OFFICER}?officerId=${contact.officer?.agid ?: ""}") },
                                    onDelete = { contact.officer?.agid?.let { id -> viewModel.deleteOfficer(id) } },
                                    onClick = {
                                        val id = contact.employee?.kgid ?: contact.officer?.agid ?: ""
                                        val isOff = contact.officer != null
                                        if (id.isNotEmpty()) navController.navigate(Routes.employeeDetailRoute(id, isOff))
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 74.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontSizeSelectorButton(
    currentFontScale: Float,
    onFontScaleSelected: (Float) -> Unit,
    onFontScaleToggle: () -> Unit,
    contentColor: Color
) {
    var showMenu by remember { mutableStateOf(false) }
    val presetSizes = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f)
    
    Box {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onFontScaleToggle, onLongClick = { showMenu = true })
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${(currentFontScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = contentColor.copy(alpha = 0.7f))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            presetSizes.forEach { size ->
                DropdownMenuItem(
                    text = { Text("${(size * 100).toInt()}%", fontWeight = if (kotlin.math.abs(size - currentFontScale) < 0.05f) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onFontScaleSelected(size); showMenu = false },
                    leadingIcon = if (kotlin.math.abs(size - currentFontScale) < 0.05f) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) } } else null
                )
            }
        }
    }
}
