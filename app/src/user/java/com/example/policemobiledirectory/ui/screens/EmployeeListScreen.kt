@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.policemobiledirectory.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.ui.theme.SecondaryYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    navController: NavController,
    viewModel: com.example.policemobiledirectory.viewmodel.EmployeeListViewModel,
    authViewModel: com.example.policemobiledirectory.viewmodel.AuthViewModel,
    settingsViewModel: com.example.policemobiledirectory.viewmodel.SettingsViewModel,
    constantsViewModel: com.example.policemobiledirectory.viewmodel.ConstantsViewModel = hiltViewModel(),
    notificationsViewModel: com.example.policemobiledirectory.viewmodel.NotificationsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PMD Dashboard",
                            modifier = Modifier.weight(1f, fill = false)
                        )
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
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
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
                    
                    // Single Font Size Button with Dropdown Menu
                    FontSizeSelectorButton(
                        currentFontScale = fontScale,
                        onFontScaleSelected = { scale ->
                            settingsViewModel.setFontScale(scale)
                        },
                        onFontScaleToggle = {
                            val presets = listOf(0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f)
                            val current = fontScale
                            val currentIndex = presets.indexOfFirst { 
                                kotlin.math.abs(it - current) < 0.05f 
                            }
                            val nextIndex = if (currentIndex >= 0 && currentIndex < presets.size - 1) {
                                currentIndex + 1
                            } else {
                                0
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
            color = MaterialTheme.colorScheme.background
        ) {
            EmployeeListContent(
                navController = navController,
                viewModel = viewModel,
                authViewModel = authViewModel,
                constantsViewModel = constantsViewModel,
                context = context,
                isAdmin = authViewModel.isAdmin.collectAsStateWithLifecycle().value,
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
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    
    // Profile verification check
    val isProfileOutdated = remember(currentUser) {
        val lastUpdate = currentUser?.updatedAt
        if (lastUpdate == null) {
            false
        } else {
            val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
            val diff = System.currentTimeMillis() - lastUpdate.time
            diff > ninetyDaysInMillis
        }
    }

    val welcomeName = remember(currentUser) {
        val name = currentUser?.name ?: ""
        if (name.isBlank()) "Officer" else name
    }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Profile Verification Warning Banner
            if (isProfileOutdated) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
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
            }

            // 2. Cyan Header Welcome Banner
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF008080), Color(0xFF00E676)) // Cyan/teal to emerald gradient
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = (14 * fontScale).sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = welcomeName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = (24 * fontScale).sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            item {
                UserDashboardActionCard(
                    title = "Search\nDirectory",
                    icon = Icons.Default.Search,
                    colorStart = Color(0xFF11998e),
                    colorEnd = Color(0xFF38ef7d),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.DIRECTORY_SEARCH) }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "Leave\nManager",
                    icon = Icons.Default.DateRange,
                    colorStart = Color(0xFF0072FF),
                    colorEnd = Color(0xFF00C6FF),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.LEAVE_DASHBOARD) }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "My Payslips",
                    icon = Icons.Default.Description,
                    colorStart = Color(0xFF008080),
                    colorEnd = Color(0xFF00D2FF),
                    fontScale = fontScale,
                    onClick = { navController.navigate("payslip_upload") }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "Useful Links",
                    icon = Icons.Default.Link,
                    colorStart = Color(0xFFF37335),
                    colorEnd = Color(0xFFFDC830),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.USEFUL_LINKS) }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "Nudi\nConverter",
                    icon = Icons.Default.TextFields,
                    colorStart = Color(0xFF8E2DE2),
                    colorEnd = Color(0xFF4A00E0),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.NUDI_CONVERTER) }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "Duty Roster",
                    icon = Icons.Default.Schedule,
                    colorStart = Color(0xFFF857A6),
                    colorEnd = Color(0xFFFF5858),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.DUTY_REGISTER) }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "Documents",
                    icon = Icons.Default.Folder,
                    colorStart = Color(0xFF4568DC),
                    colorEnd = Color(0xFFB06AB8),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.DOCUMENTS) }
                )
            }
            item {
                UserDashboardActionCard(
                    title = "Gallery",
                    icon = Icons.Default.PhotoLibrary,
                    colorStart = Color(0xFFF093FB),
                    colorEnd = Color(0xFFF5576C),
                    fontScale = fontScale,
                    onClick = { navController.navigate(Routes.GALLERY_SCREEN) }
                )
            }
        }
    }

@Composable
private fun UserDashboardActionCard(
    title: String,
    icon: ImageVector,
    colorStart: Color,
    colorEnd: Color,
    fontScale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(colorStart, colorEnd)
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = (16 * fontScale).sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart).padding(end = 50.dp)
            )
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterEnd)
            )
        }
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
