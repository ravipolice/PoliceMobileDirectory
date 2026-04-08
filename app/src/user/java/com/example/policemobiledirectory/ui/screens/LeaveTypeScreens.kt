package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.example.policemobiledirectory.viewmodel.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// CL Leave Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CLLeaveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val clEntries by leaveViewModel.clEntries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Leave Entry") },
            text = { Text("Delete this CL entry? Balance will be restored.") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text("CL Annual Limit") },
            text = {
                Column {
                    Text("Select your CL annual limit:")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(10, 15).forEach { limit ->
                            FilterChip(
                                selected = balance?.clAnnualLimit == limit,
                                onClick = {
                                    employee?.let { leaveViewModel.updateClLimit(it, limit) }
                                    showLimitDialog = false
                                },
                                label = { Text("$limit days") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Casual Leave", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLimitDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "CL Limit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    scrolledContainerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEntry("CL") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Apply CL") },
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CLStatsCard(
                    remaining = balance?.clRemaining ?: 0.0,
                    limit = balance?.clAnnualLimit ?: 15,
                    taken = (balance?.clAnnualLimit?.toDouble() ?: 15.0) - (balance?.clRemaining ?: 0.0),
                    halfDaysUsed = clEntries.count { it.isHalfDay }
                )
            }
            if (clEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No CL entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item { Text("Leave Records", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(clEntries, key = { it.id }) { entry ->
                    LeaveEntryCard(
                        entry = entry,
                        accentColor = Color(0xFF2E7D32),
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry }
                    )
                }
            }
        }
    }
}

@Composable
fun CLStatsCard(remaining: Double, limit: Int, taken: Double, halfDaysUsed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF1B5E20))))
                .padding(16.dp)
        ) {
            Column {
                Text("CL Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Remaining", "%.1f".format(remaining), Color.White)
                    StatItem("Taken", "%.1f".format(taken), Color.White)
                    StatItem("Half-Days", "$halfDaysUsed", Color.White)
                    StatItem("Limit", "$limit", Color.White)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (limit > 0) (taken / limit).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EL Leave Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ELLeaveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val elEntries by leaveViewModel.elEntries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var manualBalanceInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    val takenEntries = elEntries.filter { it.elEntryType == "taken" }
    val upcomingEntries = elEntries.filter { it.elEntryType == "upcoming" }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete EL Entry") },
            text = { Text("Delete this EL entry? Balance will be restored.") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showBalanceDialog) {
        LaunchedEffect(showBalanceDialog) {
            manualBalanceInput = balance?.elManualBalance?.toString() ?: "0"
        }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Set EL Balance") },
            text = {
                Column {
                    Text("Enter your carry-forward EL balance from service records:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualBalanceInput,
                        onValueChange = { manualBalanceInput = it },
                        label = { Text("EL Balance (days)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = manualBalanceInput.toDoubleOrNull()
                    if (v != null && v >= 0) {
                        employee?.let { leaveViewModel.updateElManualBalance(it, v) }
                        showBalanceDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Earned Leave", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBalanceDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Set EL Balance")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
                    scrolledContainerColor = Color(0xFF1565C0),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEntry("EL") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add EL") },
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ELStatsCard(
                    manualBalance = balance?.elManualBalance ?: 0.0,
                    remaining = balance?.elBalance ?: 0.0,
                    taken = takenEntries.sumOf { it.totalDays },
                    upcoming = upcomingEntries.sumOf { it.totalDays }
                )
            }
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Taken (${takenEntries.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Upcoming (${upcomingEntries.size})") })
                }
            }
            val displayEntries = if (selectedTab == 0) takenEntries else upcomingEntries
            if (displayEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No entries", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(displayEntries, key = { it.id }) { entry ->
                    LeaveEntryCard(
                        entry = entry,
                        accentColor = Color(0xFF1565C0),
                        badge = if (entry.elEntryType == "upcoming") "UPCOMING" else null,
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry }
                    )
                }
            }
        }
    }
}

@Composable
fun ELStatsCard(manualBalance: Double, remaining: Double, taken: Double, upcoming: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF0D47A1))))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("EL Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("(tap ✏ to edit)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Starting", "%.1f".format(manualBalance), Color.White)
                    StatItem("Taken", "%.1f".format(taken), Color.White)
                    StatItem("Upcoming", "%.1f".format(upcoming), Color.Yellow)
                    StatItem("Remaining", "%.1f".format(remaining), Color.White)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { if (manualBalance > 0) (taken / manualBalance).toFloat().coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HPL Leave Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HPLLeaveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val hplEntries by leaveViewModel.hplEntries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var manualBalanceInput by remember { mutableStateOf("") }

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete HPL Entry") },
            text = { Text("Delete this HPL entry? Balance will be restored.") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showBalanceDialog) {
        LaunchedEffect(showBalanceDialog) {
            manualBalanceInput = balance?.hplManualBalance?.toString()?.removeSuffix(".0") ?: "0"
        }
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Set HPL Balance") },
            text = {
                Column {
                    Text("Enter your starting HPL balance from service records:")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualBalanceInput,
                        onValueChange = { manualBalanceInput = it },
                        label = { Text("HPL Balance (days)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = manualBalanceInput.toDoubleOrNull()
                    if (v != null && v >= 0) {
                        employee?.let { leaveViewModel.updateHplManualBalance(it, v) }
                        showBalanceDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Half Pay Leave", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE65100),
                    scrolledContainerColor = Color(0xFFE65100),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showBalanceDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Set HPL Balance")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEntry("HPL") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Apply HPL") },
                containerColor = Color(0xFFE65100),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SimpleStatsCard(
                    title = "HPL Balance",
                    stats = listOf(
                        "Starting" to "%.0f".format(balance?.hplManualBalance ?: 0.0),
                        "Taken" to "%.1f".format(hplEntries.sumOf { it.totalDays }),
                        "Balance" to "%.0f".format(balance?.hplBalance ?: 0.0)
                    ),
                    gradient = listOf(Color(0xFFFB8C00), Color(0xFFE65100))
                )
            }
            if (hplEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No HPL entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item { Text("Leave Records", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(hplEntries, key = { it.id }) { entry ->
                    LeaveEntryCard(entry = entry, accentColor = Color(0xFFE65100),
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weekly Off Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyOffScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val woEntries by leaveViewModel.woEntries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val thisMonthWo = woEntries.filter { it.month == currentMonth && it.year == currentYear }

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete WO Entry") },
            text = { Text("Delete this Weekly Off entry?") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Weekly Off", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6A1B9A),
                    scrolledContainerColor = Color(0xFF6A1B9A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEntry("WO") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add WO") },
                containerColor = Color(0xFF6A1B9A),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SimpleStatsCard(
                    title = "Weekly Off",
                    stats = listOf(
                        "This Month" to "${thisMonthWo.size}/4",
                        "This Year" to "${woEntries.filter { it.year == currentYear }.size}"
                    ),
                    gradient = listOf(Color(0xFF8E24AA), Color(0xFF4A148C))
                )
            }
            if (woEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No WO entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item { Text("WO Records", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(woEntries, key = { it.id }) { entry ->
                    LeaveEntryCard(entry = entry, accentColor = Color(0xFF6A1B9A),
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MCL Screen (females only)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCLLeaveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val mclEntries by leaveViewModel.mclEntries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val usedThisMonth = balance?.mclLastUsedMonth == currentMonth && balance?.mclLastUsedYear == currentYear

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete MCL Entry") },
            text = { Text("Delete this MCL entry?") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Menstrual CL", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFC2185B),
                    scrolledContainerColor = Color(0xFFC2185B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!usedThisMonth) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToEntry("MCL") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Apply MCL") },
                    containerColor = Color(0xFFC2185B),
                    contentColor = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SimpleStatsCard(
                    title = "Menstrual CL",
                    stats = listOf(
                        "This Month" to if (usedThisMonth) "Used ✓" else "Available",
                        "This Year" to "${mclEntries.filter { it.year == currentYear }.size} used"
                    ),
                    gradient = listOf(Color(0xFFE91E63), Color(0xFF880E4F))
                )
            }
            if (usedThisMonth) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCE4EC))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFC2185B))
                            Spacer(Modifier.width(8.dp))
                            Text("MCL already availed this month", color = Color(0xFFC2185B), fontSize = 13.sp)
                        }
                    }
                }
            }
            if (mclEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No MCL entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item { Text("MCL Records", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(mclEntries, key = { it.id }) { entry ->
                    LeaveEntryCard(entry = entry, accentColor = Color(0xFFC2185B),
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CCL Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CCLLeaveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val cclEntries by leaveViewModel.cclEntries.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete CCL Entry") },
            text = { Text("Delete this CCL entry?") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Child Care Leave", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00838F),
                    scrolledContainerColor = Color(0xFF00838F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEntry("CCL") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Apply CCL") },
                containerColor = Color(0xFF00838F),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SimpleStatsCard(
                    title = "Child Care Leave",
                    stats = listOf(
                        "Used" to "%.0f days".format(balance?.cclUsed ?: 0.0),
                        "Remaining" to "%.0f days".format(730.0 - (balance?.cclUsed ?: 0.0))
                    ),
                    gradient = listOf(Color(0xFF00ACC1), Color(0xFF006064))
                )
            }
            if (cclEntries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No CCL entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item { Text("CCL Records", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(cclEntries, key = { it.id }) { entry ->
                    LeaveEntryCard(entry = entry, accentColor = Color(0xFF00838F),
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Other Leaves Screen (ML, PL, LWA)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherLeaveScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEntry: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val employee by authViewModel.currentUser.collectAsState()
    val otherEntries by leaveViewModel.otherEntries.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }
    var selectedType by remember { mutableStateOf("ML") }

    LaunchedEffect(employee) { employee?.let { leaveViewModel.refreshData(it) } }

    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Entry") },
            text = { Text("Delete this leave entry?") },
            confirmButton = {
                TextButton(onClick = {
                    employee?.let { leaveViewModel.deleteLeaveEntry(it, entry) }
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Other Leaves", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37474F),
                    scrolledContainerColor = Color(0xFF37474F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEntry(selectedType) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add $selectedType") },
                containerColor = Color(0xFF37474F),
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SimpleStatsCard(
                    title = "Other Leaves",
                    stats = listOf(
                        "ML Used" to "${balance?.maternityUsedCount ?: 0}/2",
                        "PL Used" to "${balance?.paternityUsedCount ?: 0}/2",
                        "LWA" to "${otherEntries.count { it.leaveType == "LWA" }}"
                    ),
                    gradient = listOf(Color(0xFF546E7A), Color(0xFF263238))
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ML", "PL", "LWA").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type) }
                        )
                    }
                }
            }
            val filtered = otherEntries.filter { it.leaveType == selectedType }
            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No $selectedType entries yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    LeaveEntryCard(entry = entry, accentColor = Color(0xFF37474F),
                        onEdit = { onNavigateToEdit(entry.id) },
                        onDelete = { showDeleteDialog = entry })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SimpleStatsCard(title: String, stats: List<Pair<String, String>>, gradient: List<Color>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column {
                Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    stats.forEach { (label, value) ->
                        StatItem(label, value, Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = color.copy(alpha = 0.75f), fontSize = 11.sp)
    }
}

@Composable
fun LeaveEntryCard(
    entry: LeaveEntry,
    accentColor: Color,
    badge: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${entry.leaveType}${if (entry.isHalfDay) " (Half)" else ""}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = accentColor
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(badge, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${entry.dateFrom?.let { dateFormat.format(it) } ?: "-"} → ${entry.dateTo?.let { dateFormat.format(it) } ?: "-"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "%.1f days".format(entry.totalDays) + (entry.remark?.let { " • $it" } ?: ""),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
