package com.example.policemobiledirectory.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Delete
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.viewmodel.EmployeeViewModel
import com.example.policemobiledirectory.viewmodel.LeaveUiState
import com.example.policemobiledirectory.viewmodel.LeaveViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.then(clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick
    ))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveEntryScreen(
    onNavigateBack: () -> Unit,
    preselectedType: String = "CL",
    employeeViewModel: EmployeeViewModel,
    leaveViewModel: LeaveViewModel
) {
    val context = LocalContext.current
    val currentUser by employeeViewModel.currentUser.collectAsState()
    val uiState by leaveViewModel.uiState.collectAsState()

    // Load balance when screen opens
    LaunchedEffect(currentUser) {
        currentUser?.let { leaveViewModel.refreshData(it) }
    }

    val entries by leaveViewModel.entries.collectAsState()
    
    var leaveType by remember { mutableStateOf(preselectedType) }
    var dateFrom by remember { mutableStateOf<Date?>(null) }
    var dateTo by remember { mutableStateOf<Date?>(null) }
    var remark by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isHalfDay by remember { mutableStateOf(false) }
    var elEntryType by remember { mutableStateOf("taken") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val leaveTypes = listOf("CL", "EL", "HPL", "WO", "LWA", "ML", "PL", "CCL", "MCL")

    val filteredEntries = remember(entries, leaveType) {
        entries.filter { 
            if (leaveType == "MCL") it.isMcl || it.leaveType == "MCL"
            else it.leaveType == leaveType && !it.isMcl
        }
    }

    var showDeleteDialog by remember { mutableStateOf<LeaveEntry?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is LeaveUiState.Success) {
            leaveViewModel.resetUiState()
            // Clear form but stay on screen to see history
            dateFrom = null
            dateTo = null
            remark = ""
        }
    }

    fun showDatePicker(initial: Date?, onDateSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = initial ?: Date() }
        DatePickerDialog(context, { _, y, m, d ->
            Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                onDateSelected(time)
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun calculateDays(): Double {
        if (isHalfDay) return 0.5
        val from = dateFrom ?: return 0.0
        val to = dateTo ?: return 0.0
        val diff = to.time - from.time
        return (diff / (1000 * 60 * 60 * 24)).toDouble() + 1
    }

    val days = calculateDays()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$leaveType Entry & History", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- ENTRY SECTION ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("New Registration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = leaveType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Leave Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            leaveTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        leaveType = type
                                        expanded = false
                                        isHalfDay = false
                                        elEntryType = "taken"
                                    }
                                )
                            }
                        }
                    }

                    if (leaveType == "EL") {
                        Text("EL Entry Type", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("taken" to "Taken", "upcoming" to "Upcoming").forEach { (value, label) ->
                                FilterChip(
                                    selected = elEntryType == value,
                                    onClick = { elEntryType = value },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    if (leaveType == "CL") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isHalfDay, onCheckedChange = {
                                isHalfDay = it
                                if (it) dateTo = dateFrom
                            })
                            Text("Half Day (0.5 days)")
                        }
                    }

                    OutlinedTextField(
                        value = dateFrom?.let { dateFormat.format(it) } ?: "Tap to select",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHalfDay) "Date" else "From Date") },
                        modifier = Modifier.fillMaxWidth().noRippleClickable {
                            showDatePicker(dateFrom) {
                                dateFrom = it
                                if (isHalfDay || leaveType in listOf("WO", "MCL")) dateTo = it
                            }
                        }
                    )

                    if (!isHalfDay && leaveType !in listOf("WO", "MCL")) {
                        OutlinedTextField(
                            value = dateTo?.let { dateFormat.format(it) } ?: "Tap to select",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("To Date") },
                            modifier = Modifier.fillMaxWidth().noRippleClickable {
                                showDatePicker(dateTo ?: dateFrom) { dateTo = it }
                            }
                        )
                    }

                    if (days > 0) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Text(
                                text = "Duration: ${"%.1f".format(days)} day${if (days != 1.0) "s" else ""}",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = { Text("Remark (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    if (uiState is LeaveUiState.Error) {
                        Text(text = (uiState as LeaveUiState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp))
                    }

                    Button(
                        onClick = {
                            val emp = currentUser ?: return@Button
                            val from = dateFrom ?: return@Button
                            val to = if (isHalfDay || leaveType in listOf("WO", "MCL")) from else (dateTo ?: return@Button)
                            val cal = Calendar.getInstance().apply { time = from }
                            val entry = LeaveEntry(
                                kgid = emp.kgid,
                                dateFrom = from,
                                dateTo = to,
                                totalDays = days,
                                leaveType = if (leaveType == "MCL") "CL" else leaveType,
                                remark = remark.ifBlank { null },
                                isHalfDay = isHalfDay,
                                isMcl = leaveType == "MCL",
                                elEntryType = if (leaveType == "EL") elEntryType else "taken",
                                year = cal.get(Calendar.YEAR),
                                month = cal.get(Calendar.MONTH) + 1
                            )
                            leaveViewModel.applyLeave(emp, entry)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = dateFrom != null && (isHalfDay || leaveType in listOf("WO", "MCL") || dateTo != null) && uiState !is LeaveUiState.Loading
                    ) {
                        if (uiState is LeaveUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Save Leave Entry")
                        }
                    }
                }
            }

            // --- HISTORY SECTION ---
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Previous $leaveType Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (filteredEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No records for $leaveType", color = Color.Gray)
                }
            } else {
                filteredEntries.forEach { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (entry.isMcl) "Menstrual CL" else entry.leaveType,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val dateStr = if (entry.dateFrom == entry.dateTo) {
                                    entry.dateFrom?.let { dateFormat.format(it) } ?: ""
                                } else {
                                    "${entry.dateFrom?.let { dateFormat.format(it) } ?: ""} - ${entry.dateTo?.let { dateFormat.format(it) } ?: ""}"
                                }
                                Text(text = dateStr, fontSize = 14.sp)
                                Text(text = "Duration: ${entry.totalDays} days", fontSize = 12.sp, color = Color.Gray)
                                entry.remark?.let {
                                    Text(text = "Note: $it", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                            IconButton(onClick = { showDeleteDialog = entry }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this leave entry? This will restore your leave balance.") },
            confirmButton = {
                TextButton(onClick = {
                    val entry = showDeleteDialog!!
                    val emp = currentUser ?: return@TextButton
                    leaveViewModel.deleteLeaveEntry(emp, entry)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
