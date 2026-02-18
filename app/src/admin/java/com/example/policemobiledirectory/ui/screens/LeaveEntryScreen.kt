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

    var leaveType by remember { mutableStateOf(preselectedType) }
    var dateFrom by remember { mutableStateOf<Date?>(null) }
    var dateTo by remember { mutableStateOf<Date?>(null) }
    var remark by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isHalfDay by remember { mutableStateOf(false) }
    var elEntryType by remember { mutableStateOf("taken") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val leaveTypes = listOf("CL", "EL", "HPL", "WO", "LWA", "ML", "PL", "CCL", "MCL")

    LaunchedEffect(uiState) {
        if (uiState is LeaveUiState.Success) {
            leaveViewModel.resetUiState()
            onNavigateBack()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Leave Entry", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Leave Type Dropdown
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

            // EL: Taken / Upcoming selector
            if (leaveType == "EL") {
                Text("EL Entry Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("taken" to "Taken (deduct)", "upcoming" to "Upcoming (track only)").forEach { (value, label) ->
                        FilterChip(
                            selected = elEntryType == value,
                            onClick = { elEntryType = value },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // CL: Half-day option
            if (leaveType == "CL") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHalfDay, onCheckedChange = {
                        isHalfDay = it
                        if (it) dateTo = dateFrom
                    })
                    Text("Half Day (0.5 days)")
                }
            }

            // Date From
            OutlinedTextField(
                value = dateFrom?.let { dateFormat.format(it) } ?: "Tap to select",
                onValueChange = {},
                readOnly = true,
                label = { Text(if (isHalfDay) "Date" else "From Date") },
                modifier = Modifier.fillMaxWidth().noRippleClickable {
                    showDatePicker(dateFrom) {
                        dateFrom = it
                        if (isHalfDay) dateTo = it
                    }
                }
            )

            // Date To (not for half-day or single-day leaves)
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

            // Days preview
            val days = calculateDays()
            if (days > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        text = "Duration: ${"%.1f".format(days)} day${if (days != 1.0) "s" else ""}",
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Remark
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                label = { Text("Remark (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Error display
            if (uiState is LeaveUiState.Error) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = (uiState as LeaveUiState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Save button
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
                modifier = Modifier.fillMaxWidth(),
                enabled = dateFrom != null &&
                        (isHalfDay || leaveType in listOf("WO", "MCL") || dateTo != null) &&
                        uiState !is LeaveUiState.Loading
            ) {
                if (uiState is LeaveUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Save Leave Entry")
                }
            }
        }
    }
}
