package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveStatistics
import com.example.policemobiledirectory.viewmodel.LeaveUiState
import com.example.policemobiledirectory.viewmodel.LeaveViewModel
import com.example.policemobiledirectory.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCl: () -> Unit,
    onNavigateToEl: () -> Unit,
    onNavigateToHpl: () -> Unit,
    onNavigateToWo: () -> Unit,
    onNavigateToCcl: () -> Unit,
    onNavigateToMcl: () -> Unit,
    onNavigateToOther: () -> Unit,
    onNavigateToApplyLeave: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToRules: () -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel,
    onGoogleSignInClicked: () -> Unit,
    onDrivePermissionRequest: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val statistics by leaveViewModel.statistics.collectAsState()
    val uiState by leaveViewModel.uiState.collectAsState()
    val isDrivePermissionGranted by leaveViewModel.isDrivePermissionGranted.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showEditClLimitDialog by remember { mutableStateOf(false) }
    var showEditElBalanceDialog by remember { mutableStateOf(false) }
    var showEditHplBalanceDialog by remember { mutableStateOf(false) }
    var elText by remember { mutableStateOf("0") }
    var hplText by remember { mutableStateOf("0") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                leaveViewModel.checkDrivePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUser) {
        currentUser?.let { 
            leaveViewModel.refreshData(it)
            leaveViewModel.checkDrivePermission()
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        when (state) {
            is LeaveUiState.Error -> {
                // We removed the automatic onDrivePermissionRequest() from here
                // to prevent "repeated permission requests". 
                // Manual backup/restore already handle permission checks.
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                leaveViewModel.resetUiState()
            }
            is LeaveUiState.BackupSuccess -> {
                snackbarHostState.showSnackbar("Backup successful!")
                leaveViewModel.resetUiState()
            }
            is LeaveUiState.RestoreSuccess -> {
                snackbarHostState.showSnackbar("Data restored successfully!")
                leaveViewModel.resetUiState()
            }
            is LeaveUiState.Success -> {
                leaveViewModel.resetUiState()
            }
            else -> {}
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Confirm Restore") },
            text = { Text("This will overwrite your local leave data with the backup from Google Drive. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    currentUser?.let { leaveViewModel.restoreData(it) }
                    showRestoreDialog = false
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditClLimitDialog) {
        AlertDialog(
            onDismissRequest = { showEditClLimitDialog = false },
            title = { Text("Casual Leave Limit") },
            text = { Text("Select your annual Casual Leave limit:") },
            confirmButton = {},
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        currentUser?.let { user -> leaveViewModel.updateClLimit(user, 10) }
                        showEditClLimitDialog = false
                    }) {
                        Text("10 Days")
                    }
                    TextButton(onClick = {
                        currentUser?.let { user -> leaveViewModel.updateClLimit(user, 15) }
                        showEditClLimitDialog = false
                    }) {
                        Text("15 Days")
                    }
                }
            }
        )
    }

    if (showEditElBalanceDialog) {
        LaunchedEffect(balance) {
            elText = balance?.elManualBalance?.toString()?.removeSuffix(".0") ?: "0"
        }
        AlertDialog(
            onDismissRequest = { showEditElBalanceDialog = false },
            title = { Text("Edit EL Balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your carried-over EL balance:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = elText,
                        onValueChange = { elText = it },
                        label = { Text("Earned Leave (EL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    currentUser?.let { user ->
                        val newEl = elText.toDoubleOrNull() ?: balance?.elManualBalance ?: 0.0
                        leaveViewModel.updateElManualBalance(user, newEl)
                    }
                    showEditElBalanceDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditElBalanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditHplBalanceDialog) {
        LaunchedEffect(balance) {
            hplText = balance?.hplManualBalance?.toString()?.removeSuffix(".0") ?: "0"
        }
        AlertDialog(
            onDismissRequest = { showEditHplBalanceDialog = false },
            title = { Text("Edit HPL Balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your starting HPL balance from service records:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = hplText,
                        onValueChange = { hplText = it },
                        label = { Text("Half Pay Leave (HPL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    currentUser?.let { user ->
                        val newHpl = hplText.toDoubleOrNull() ?: balance?.hplManualBalance ?: 0.0
                        leaveViewModel.updateHplManualBalance(user, newHpl)
                    }
                    showEditHplBalanceDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHplBalanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Leave Register", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Refresh Data") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                currentUser?.let { leaveViewModel.refreshData(it) }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave Rules") },
                            leadingIcon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                            onClick = {
                                onNavigateToRules()
                                showMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Backup to Drive") },
                            leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                            onClick = {
                                currentUser?.let { leaveViewModel.backupData(it) }
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restore from Drive") },
                            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                            onClick = {
                                showRestoreDialog = true
                                showMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("History / Reports") },
                            leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                            onClick = {
                                onNavigateToReports()
                                showMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F7)).padding(paddingValues)) {
            if (uiState is LeaveUiState.Loading && balance == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Stats Card
                    LeaveOverviewCard(balance, statistics)

                    // Grid of leave types
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LeaveTypeTile(
                                title = "Casual Leave",
                                label = "of ${balance?.clAnnualLimit ?: 15} days",
                                value = balance?.clRemaining?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "0",
                                icon = Icons.Default.WbSunny,
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),
                                modifier = Modifier.weight(1f),
                                actionIcon = Icons.Default.Edit,
                                onActionClick = { showEditClLimitDialog = true },
                                onClick = onNavigateToCl
                            )
                            LeaveTypeTile(
                                title = "Earned Leave",
                                label = "days remaining",
                                value = balance?.elBalance?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.0f".format(it) } ?: "0",
                                icon = Icons.Default.Star,
                                colors = listOf(Color(0xFF2196F3), Color(0xFF1565C0)),
                                modifier = Modifier.weight(1f),
                                actionIcon = Icons.Default.Edit,
                                onActionClick = { showEditElBalanceDialog = true },
                                onClick = onNavigateToEl
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LeaveTypeTile(
                                title = "Half Pay Leave",
                                label = "days remaining",
                                value = balance?.hplBalance?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.0f".format(it) } ?: "0",
                                icon = Icons.Default.Schedule,
                                colors = listOf(Color(0xFFFF9800), Color(0xFFE65100)),
                                modifier = Modifier.weight(1f),
                                actionIcon = Icons.Default.Edit,
                                onActionClick = { showEditHplBalanceDialog = true },
                                onClick = onNavigateToHpl
                            )
                            LeaveTypeTile(
                                title = "Weekly Off",
                                label = "this year",
                                value = statistics?.leaveTypeBreakdown?.get("WO")?.toInt()?.toString() ?: "0",
                                icon = Icons.Default.Weekend,
                                colors = listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A)),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToWo
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LeaveTypeTile(
                                title = "Child Care Leave",
                                label = "days used",
                                value = balance?.cclUsed?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.0f".format(it) } ?: "0",
                                icon = Icons.Default.ChildCare,
                                colors = listOf(Color(0xFF009688), Color(0xFF00695C)),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToCcl
                            )
                            val othersTotal = (statistics?.leaveTypeBreakdown?.get("ML") ?: 0.0) + (statistics?.leaveTypeBreakdown?.get("PL") ?: 0.0) + (statistics?.leaveTypeBreakdown?.get("LWA") ?: 0.0)
                            LeaveTypeTile(
                                title = "Others / LWA",
                                label = "days this year",
                                value = if (othersTotal % 1.0 == 0.0) othersTotal.toInt().toString() else othersTotal.toString(),
                                icon = Icons.Default.MoreHoriz,
                                colors = listOf(Color(0xFF455A64), Color(0xFF263238)),
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToOther
                            )
                        }
                    }

                    // Navigation Banner
                    ApplyLeaveBanner(onClick = onNavigateToApplyLeave)
                    
                }
            }

            if (uiState is LeaveUiState.Loading && balance != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun LeaveTypeTile(
    title: String,
    label: String,
    value: String,
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(colors))
                .padding(12.dp)
        ) {
            // Top row: edit icon (start) + title (end)
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionIcon != null && onActionClick != null) {
                    IconButton(
                        onClick = onActionClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = "Edit",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Centre-left: balance number + subtitle
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(top = 4.dp)
            ) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            
            // Background icon (bottom-end)
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(64.dp).align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun ApplyLeaveBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF512DA8), Color(0xFF9C27B0))
                    )
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("New Leave Entry", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Unified application form", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LeaveOverviewCard(balance: LeaveBalance?, statistics: LeaveStatistics?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val takenText = statistics?.totalTaken?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "0"
            val remainingText = statistics?.totalRemaining?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "0"
            
            OverviewStat(label = "Taken", value = takenText, icon = Icons.Default.EventBusy, color = Color(0xFFD32F2F))
            
            VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.5f))
            
            OverviewStat(label = "Remaining", value = remainingText, icon = Icons.Default.EventAvailable, color = Color(0xFF388E3C))
            
            VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.5f))
            
            OverviewStat(label = "Most Used", value = statistics?.mostUsedType?.ifEmpty { "-" } ?: "-", icon = Icons.Default.TrendingUp, color = Color(0xFF1976D2))
        }
    }
}

@Composable
fun OverviewStat(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

