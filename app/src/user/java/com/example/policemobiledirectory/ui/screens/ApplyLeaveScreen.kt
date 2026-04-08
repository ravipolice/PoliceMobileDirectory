package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.policemobiledirectory.ui.components.ApplyLeaveForm
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.example.policemobiledirectory.viewmodel.LeaveViewModel
import com.example.policemobiledirectory.viewmodel.LeaveUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel,
    initialType: String = "CL"
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val uiState by leaveViewModel.uiState.collectAsState()

    var showSuccessMessage by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LeaveUiState.Success) {
            showSuccessMessage = true
            leaveViewModel.resetUiState()
            kotlinx.coroutines.delay(1500)
            onSaveSuccess()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Apply for Leave", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (showSuccessMessage) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF43A047),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Leave saved successfully!",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            ApplyLeaveForm(
                currentUser = currentUser,
                leaveViewModel = leaveViewModel,
                uiState = uiState,
                initialType = initialType
            )
        }
    }
}
