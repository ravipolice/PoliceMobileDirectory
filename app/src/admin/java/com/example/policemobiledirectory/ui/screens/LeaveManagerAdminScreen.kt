package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.viewmodel.LeaveManagerAdminViewModel
import com.example.policemobiledirectory.utils.OperationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveManagerAdminScreen(
    navController: NavController,
    viewModel: LeaveManagerAdminViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val status by viewModel.status.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(status) {
        val state = status
        if (state is OperationStatus.Error) {
            snackbarHostState.showSnackbar(state.message)
        } else if (state is OperationStatus.Success<*>) {
            val msg = state.data as? String ?: "Operation successful"
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Leave Register", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Approvals (${pendingUsers.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Departments") }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> PendingApprovalsList(pendingUsers, viewModel)
                    1 -> DepartmentsList(departments, viewModel)
                }

                if (status is OperationStatus.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun PendingApprovalsList(users: List<Map<String, Any>>, viewModel: LeaveManagerAdminViewModel) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending registrations", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(user["name"]?.toString() ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("KGID: ${user["id"]}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        DetailRow(Icons.Default.Business, user["department"]?.toString() ?: "-")
                        DetailRow(Icons.Default.LocationOn, user["district"]?.toString() ?: "-")
                        DetailRow(Icons.Default.Email, user["email"]?.toString() ?: "-")
                        DetailRow(Icons.Default.Phone, user["phone"]?.toString() ?: "-")
                        
                        val lastActive = (user["lastActive"] as? Number)?.toLong()
                        if (lastActive != null && lastActive > 0) {
                            DetailRow(Icons.Default.Event, "Last Active: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(lastActive))}")
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.approveUser(user["id"].toString()) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Approve")
                            }
                            OutlinedButton(
                                onClick = { viewModel.rejectUser(user["id"].toString()) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Close, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DepartmentsList(departments: List<String>, viewModel: LeaveManagerAdminViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newDeptName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (departments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No departments found", color = Color.Gray)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(departments) { dept ->
                    ListItem(
                        headlineContent = { Text(dept) },
                        leadingContent = { Icon(Icons.Default.Business, null) },
                        trailingContent = { 
                            // Add delete if needed, for now just list
                        }
                    )
                    HorizontalDivider()
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Add, "Add Department")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Department") },
                text = {
                    OutlinedTextField(
                        value = newDeptName,
                        onValueChange = { newDeptName = it },
                        label = { Text("Department Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newDeptName.isNotBlank()) {
                            viewModel.addDepartment(newDeptName)
                            newDeptName = ""
                            showAddDialog = false
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
