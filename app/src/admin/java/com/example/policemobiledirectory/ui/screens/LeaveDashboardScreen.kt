package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.viewmodel.LeaveUiState
import com.example.policemobiledirectory.viewmodel.LeaveViewModel

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
    onNavigateToReports: () -> Unit,
    employeeViewModel: EmployeeViewModel,
    leaveViewModel: LeaveViewModel
) {
    val currentUser by employeeViewModel.currentUser.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()
    val statistics by leaveViewModel.statistics.collectAsState()
    val uiState by leaveViewModel.uiState.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { leaveViewModel.refreshData(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Leave Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { currentUser?.let { leaveViewModel.refreshData(it) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToReports) {
                        Icon(Icons.Default.BarChart, contentDescription = "Reports")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is LeaveUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is LeaveUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null,
                            modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text((uiState as LeaveUiState.Error).message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { currentUser?.let { leaveViewModel.refreshData(it) } }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary header spanning full width
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        LeaveOverviewCard(balance = balance, statistics = statistics)
                    }

                    // CL Tile
                    item {
                        LeaveTile(
                            title = "Casual Leave",
                            balance = balance?.clRemaining?.let { "%.1f".format(it) } ?: "-",
                            subtitle = "of ${balance?.clAnnualLimit ?: 15} days",
                            icon = Icons.Default.WbSunny,
                            gradient = listOf(Color(0xFF43A047), Color(0xFF1B5E20)),
                            onClick = onNavigateToCl
                        )
                    }

                    // EL Tile
                    item {
                        LeaveTile(
                            title = "Earned Leave",
                            balance = balance?.elBalance?.let { "%.0f".format(it) } ?: "-",
                            subtitle = "days remaining",
                            icon = Icons.Default.Star,
                            gradient = listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)),
                            onClick = onNavigateToEl
                        )
                    }

                    // HPL Tile
                    item {
                        LeaveTile(
                            title = "Half Pay Leave",
                            balance = balance?.hplBalance?.let { "%.0f".format(it) } ?: "-",
                            subtitle = "days remaining",
                            icon = Icons.Default.Schedule,
                            gradient = listOf(Color(0xFFFB8C00), Color(0xFFE65100)),
                            onClick = onNavigateToHpl
                        )
                    }

                    // WO Tile
                    item {
                        LeaveTile(
                            title = "Weekly Off",
                            balance = statistics?.leaveTypeBreakdown?.get("WO")?.toString() ?: "0",
                            subtitle = "this year",
                            icon = Icons.Default.Weekend,
                            gradient = listOf(Color(0xFF8E24AA), Color(0xFF4A148C)),
                            onClick = onNavigateToWo
                        )
                    }

                    // CCL Tile
                    item {
                        LeaveTile(
                            title = "Child Care Leave",
                            balance = balance?.cclUsed?.let { "%.0f".format(it) } ?: "0",
                            subtitle = "days used",
                            icon = Icons.Default.ChildCare,
                            gradient = listOf(Color(0xFF00ACC1), Color(0xFF006064)),
                            onClick = onNavigateToCcl
                        )
                    }

                    // MCL Tile — females only
                    if (currentUser?.gender?.lowercase() == "female") {
                        item {
                            val cal = java.util.Calendar.getInstance()
                            val usedThisMonth = balance?.mclLastUsedMonth == cal.get(java.util.Calendar.MONTH) + 1
                                    && balance?.mclLastUsedYear == cal.get(java.util.Calendar.YEAR)
                            LeaveTile(
                                title = "Menstrual CL",
                                balance = if (usedThisMonth) "Used" else "Available",
                                subtitle = "this month",
                                icon = Icons.Default.Favorite,
                                gradient = listOf(Color(0xFFE91E63), Color(0xFF880E4F)),
                                onClick = onNavigateToMcl
                            )
                        }
                    }

                    // Other Leaves Tile
                    item {
                        LeaveTile(
                            title = "Maternity / Paternity / LWA",
                            balance = "${(statistics?.leaveTypeBreakdown?.get("ML") ?: 0) +
                                    (statistics?.leaveTypeBreakdown?.get("PL") ?: 0) +
                                    (statistics?.leaveTypeBreakdown?.get("LWA") ?: 0)}",
                            subtitle = "days this year",
                            icon = Icons.Default.MoreHoriz,
                            gradient = listOf(Color(0xFF546E7A), Color(0xFF263238)),
                            onClick = onNavigateToOther
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaveTile(
    title: String,
    balance: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(16.dp)
        ) {
            // Title in Top-Right
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Balance and Subtitle in Center-Left
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = balance,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            
            // Large Background Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun LeaveOverviewCard(
    balance: LeaveBalance?,
    statistics: com.example.policemobiledirectory.model.LeaveStatistics?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OverviewStat(
                label = "Taken",
                value = statistics?.totalTaken?.toString() ?: "0",
                icon = Icons.Default.EventBusy,
                color = MaterialTheme.colorScheme.error
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            OverviewStat(
                label = "Remaining",
                value = statistics?.totalRemaining?.toString() ?: "-",
                icon = Icons.Default.EventAvailable,
                color = Color(0xFF43A047)
            )
            VerticalDivider(modifier = Modifier.height(48.dp))
            OverviewStat(
                label = "Most Used",
                value = statistics?.mostUsedType?.ifEmpty { "-" } ?: "-",
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun OverviewStat(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
