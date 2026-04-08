package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.ConstantsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDutyRolesScreen(
    navController: NavController,
    viewModel: ConstantsViewModel = hiltViewModel()
) {
    val units by viewModel.units.collectAsState()
    val allRoles by viewModel.subSectionList.collectAsState()
    val mapping by viewModel.dutyRoleMapping.collectAsState()
    val refreshStatus by viewModel.refreshStatus.collectAsState()

    var selectedUnit by remember { mutableStateOf<String?>(null) }
    var selectedRoles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    // Initialize selectedRoles when a unit is selected
    LaunchedEffect(selectedUnit, mapping) {
        selectedUnit?.let { unit ->
            selectedRoles = mapping[unit]?.toSet() ?: emptySet()
        }
    }

    LaunchedEffect(refreshStatus) {
        if (refreshStatus is OperationStatus.Success) {
            showSuccessSnackbar = true
            viewModel.resetRefreshStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Duty Roles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedUnit != null) {
                        TextButton(
                            onClick = { 
                                selectedUnit?.let { viewModel.updateDutyRoleMapping(it, selectedRoles.toList()) }
                            },
                            enabled = refreshStatus !is OperationStatus.Loading
                        ) {
                            Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Success Snackbar/Banner
            if (showSuccessSnackbar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Mapping saved successfully", color = Color.White)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showSuccessSnackbar = false }) {
                            Text("DISMISS", color = Color.White)
                        }
                    }
                }
            }

            if (refreshStatus is OperationStatus.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // Left Column: Units List
                LazyColumn(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    item {
                        PaddingHeader("Functional Units")
                    }
                    items(units) { unit ->
                        val isSelected = selectedUnit == unit
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUnit = unit }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = unit,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                // Right Column: Roles for Selected Unit
                Column(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    if (selectedUnit == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a unit to manage roles", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                PaddingHeader("Select Duty Roles for ${selectedUnit}")
                            }
                            items(allRoles) { role ->
                                val isChecked = selectedRoles.contains(role)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            selectedRoles = if (isChecked) selectedRoles - role else selectedRoles + role
                                        }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null // Handled by row click
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(role)
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaddingHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp)
    )
}
