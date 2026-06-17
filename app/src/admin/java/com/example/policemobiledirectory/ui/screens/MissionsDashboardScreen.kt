package com.example.policemobiledirectory.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.data.remote.Mission
import com.example.policemobiledirectory.viewmodel.MissionsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsDashboardScreen(
    navController: NavController,
    viewModel: MissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Indian Missions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nandija.vercel.app/missions"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore or log
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Open Web Dashboard",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.fetchMissions(forceRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
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
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        var missionToEdit by remember { mutableStateOf<Mission?>(null) }

        if (missionToEdit != null) {
            EditMissionDialog(
                mission = missionToEdit!!,
                onDismiss = { missionToEdit = null },
                onSave = { updatedMission ->
                    viewModel.updateMission(updatedMission)
                    missionToEdit = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Syncing with Google Sheet...")
                    }
                }
            )
        }


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA)), // Light gray background
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            // 2. Stats Section (Per Sketch)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Missions",
                        value = uiState.totalCount.toString(),
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF2C5590), Color(0xFF1E3D6B))
                    )
                    StatCard(
                        title = "Resident Missions",
                        value = uiState.residentCount.toString(),
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                    )
                }
            }

            // 3. Live Search Section
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(12.dp)),
                    placeholder = { Text("Search by country, city or mission name…", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF2C5590)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF2C5590).copy(alpha = 0.5f)
                    )
                )
            }

            // 4. Results List
            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
            } else if (uiState.filteredMissions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No missions found for selection", color = Color.Gray)
                    }
                }
            } else {
                items(uiState.filteredMissions) { mission ->
                    MissionDetailFormCard(
                        mission = mission,
                        onEditClick = { missionToEdit = it }
                    )
                }
            }

            
            // Removed extra spacer to reduce bottom padding

        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, gradientColors: List<Color>) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(gradientColors))
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}



@Composable
fun MissionDetailFormCard(mission: Mission, onEditClick: (Mission) -> Unit) {

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${mission.country.uppercase()} - ${mission.city}",
                        color = Color(0xFF263238),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (mission.type.isNotBlank()) {
                            Text(
                                text = mission.type,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(text = "•", color = Color.LightGray)
                        }
                        if (mission.region.isNotBlank()) {
                            Text(
                                text = mission.region,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeColor = if (mission.status == "Resident") Color(0xFF2E7D32) else Color(0xFFEF6C00)
                    val badgeBgColor = if (mission.status == "Resident") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    Surface(
                        color = badgeBgColor,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = mission.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    IconButton(onClick = { onEditClick(mission) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Mission",
                            tint = Color(0xFF2C5590),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            FormField(label = "Official Name", value = mission.name)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            ColiBadge(coli = mission.costOfLiving)
                        }
                    }
                    
                    if (mission.notes.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8F9FA))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "REMARKS / NOTES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mission.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF37474F)
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
fun FormField(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF2C5590)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            Text(
                text = if (value.isBlank()) "---" else value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ColiBadge(coli: String) {
    val numericColi = coli.toDoubleOrNull() ?: 0.0
    val (label, color) = when {
        numericColi == 0.0 -> "PENDING" to Color.Gray
        numericColi < 25 -> "AFFORDABLE" to Color(0xFF4CAF50)
        numericColi < 55 -> "MODERATE" to Color(0xFF2196F3)
        numericColi < 80 -> "HIGH" to Color(0xFFFF9800)
        else -> "PREMIUM" to Color(0xFFF44336)
    }

    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "COLI INDEX",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.ExtraBold
                )
                if (coli.isNotBlank()) {
                    Text(
                        text = " ($coli)",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun EditMissionDialog(
    mission: Mission,
    onDismiss: () -> Unit,
    onSave: (Mission) -> Unit
) {
    var status by remember { mutableStateOf(mission.status) }
    var notes by remember { mutableStateOf(mission.notes) }
    var coli by remember { mutableStateOf(mission.costOfLiving) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Mission: ${mission.country} / ${mission.city}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusOptions = listOf("Resident", "Non-Resident")
                    statusOptions.forEach { option ->
                        val isSelected = status == option
                        val buttonColor = if (isSelected) Color(0xFF2C5590) else Color(0xFFF5F7F8)
                        val textColor = if (isSelected) Color.White else Color.Black
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(buttonColor)
                                .clickable { status = option }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = option, color = textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = coli,
                    onValueChange = { coli = it },
                    label = { Text("COLI Index") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarks / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(mission.copy(status = status, notes = notes, costOfLiving = coli)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C5590))
            ) {
                Text("SAVE TO SHEET")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray)
            }
        }
    )
}



