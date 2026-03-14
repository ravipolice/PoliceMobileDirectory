package com.example.policemobiledirectory.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.navigation.Routes
import com.example.policemobiledirectory.repository.RepoResult
import com.example.policemobiledirectory.ui.components.CommonEmployeeForm
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel

@Composable
fun MyProfileEditScreen(
    navController: NavController? = null,
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentEmployee by viewModel.currentUser.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val photoUploadStatus by viewModel.photoUploadStatus.collectAsState()
    val isLoading = saveStatus is RepoResult.Loading || 
                    photoUploadStatus is OperationStatus.Loading

    // Handle back button to navigate to home screen
    BackHandler(enabled = navController != null) {
        navController?.navigate(Routes.EMPLOYEE_LIST) {
            popUpTo(Routes.EMPLOYEE_LIST) { inclusive = false }
            launchSingleTop = true
        }
    }

    // Show feedback messages
    LaunchedEffect(saveStatus) {
        when (val status = saveStatus) {
            is RepoResult.Success -> {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveStatus()
                navController?.popBackStack()
            }
            is RepoResult.Error -> {
                Toast.makeText(context, status.message ?: "Failed to update profile", Toast.LENGTH_LONG).show()
                viewModel.resetSaveStatus()
            }
            else -> Unit
        }
    }

    // Check if profile is outdated (older than 90 days)
    val isProfileOutdated = androidx.compose.runtime.remember(currentEmployee) {
        val lastUpdate = currentEmployee?.updatedAt
        if (lastUpdate == null) {
            true
        } else {
            val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
            val diff = System.currentTimeMillis() - lastUpdate.time
            diff > ninetyDaysInMillis
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isProfileOutdated && currentEmployee != null) {
            OutdatedProfileNotice()
        }
    
        CommonEmployeeForm(
            isAdmin = false,
            isSelfEdit = true,
            isRegistration = false,
            initialEmployee = currentEmployee,
            onNavigateToTerms = null,
            onSubmit = { emp: Employee, photo: Uri? ->
                viewModel.saveEmployee(emp, photo)
            },
            onRegisterSubmit = null,
            isLoading = isLoading
        )
    }
}

@Composable
fun OutdatedProfileNotice() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Please verify your profile details. It has been over 3 months since the last update.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
