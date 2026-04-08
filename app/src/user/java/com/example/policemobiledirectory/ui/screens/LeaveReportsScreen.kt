package com.example.policemobiledirectory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack as _ArrowBack // avoid name collision if accidentally used
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.model.LeaveCreditLog
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.viewmodel.AuthViewModel
import com.example.policemobiledirectory.viewmodel.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveReportsScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel,
    leaveViewModel: LeaveViewModel
) {
    val balance by leaveViewModel.balance.collectAsState()
    val statistics by leaveViewModel.statistics.collectAsState()
    val creditLogs by leaveViewModel.creditLogs.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
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
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                Text(
                    text = "Usage by Type ($currentYear)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            val groupedUsage = statistics?.leaveTypeBreakdown ?: emptyMap()

            if (groupedUsage.isEmpty()) {
                item { 
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    Text("No usage recorded for $currentYear", color = Color.Gray) 
                }
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

            if (creditLogs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Credit History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(creditLogs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (log.type) {
                                        "EL_CREDIT" -> "EL Credited"
                                        "HPL_CREDIT" -> "HPL Credited"
                                        "CL_RESET" -> "CL Reset"
                                        else -> log.type
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                log.date?.let {
                                    Text(
                                        text = dateFormatter.format(it),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Text(
                                text = (if (log.amount > 0) "+" else "") + "${log.amount} Days",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (log.amount > 0) Color(0xFF388E3C) else Color.Gray
                            )
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
