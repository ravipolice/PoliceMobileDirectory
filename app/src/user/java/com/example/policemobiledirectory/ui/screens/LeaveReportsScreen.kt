package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.viewmodel.LeaveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveReportsScreen(
    onNavigateBack: () -> Unit,
    employeeViewModel: EmployeeViewModel,
    leaveViewModel: LeaveViewModel
) {
    val entries by leaveViewModel.entries.collectAsState()
    val balance by leaveViewModel.balance.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Lifetime Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                balance?.let {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ReportRow("Earned Leave (EL)", "${it.elBalance}")
                            ReportRow("Half Pay Leave (HPL)", "${it.hplBalance}")
                            ReportRow("Child Care Leave Used", "${it.cclUsed} days")
                            ReportRow("Maternity Usage", "${it.maternityUsedCount}/2")
                            ReportRow("Paternity Usage", "${it.paternityUsedCount}/2")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text(
                    text = "Usage by Type (This Year)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val yearEntries = entries.filter { it.year == currentYear }
            val groupedUsage = yearEntries.groupBy { it.leaveType }
                .mapValues { it.value.sumOf { entry -> entry.totalDays } }

            if (groupedUsage.isEmpty()) {
                item { Text("No usage recorded for $currentYear", color = Color.Gray) }
            } else {
                items(groupedUsage.toList()) { (type, days) ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = type, fontWeight = FontWeight.Medium)
                            Text(text = "$days Days", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}
