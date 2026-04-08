package com.example.policemobiledirectory.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.model.Employee
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
fun ApplyLeaveForm(
    currentUser: Employee?,
    leaveViewModel: LeaveViewModel,
    uiState: LeaveUiState,
    initialType: String = "CL"
) {
    val context = LocalContext.current
    
    var leaveType by remember { mutableStateOf(initialType) }
    var dateFrom by remember { mutableStateOf<Date?>(null) }
    var dateTo by remember { mutableStateOf<Date?>(null) }
    var remark by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isHalfDay by remember { mutableStateOf(false) }
    var elEntryType by remember { mutableStateOf("taken") }
    var hasMedicalCertificate by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    
    val leaveTypes = remember(currentUser) {
        val baseTypes = mutableListOf("CL", "EL", "HPL", "WO", "LWA", "CML", "LND")
        if (currentUser?.gender?.lowercase() == "female") {
            baseTypes.addAll(listOf("ML", "CCL", "MCL"))
        } else {
            baseTypes.addAll(listOf("PL"))
        }
        baseTypes.toList()
    }

    fun showDatePicker(initial: Date?, onDateSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = initial ?: Date() }
        DatePickerDialog(context, { _, y, m, d ->
            val result = Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(result.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun calculateDays(): Double {
        if (isHalfDay) return 0.5
        val from = dateFrom ?: return 0.0
        val to = if (leaveType in listOf("WO", "MCL")) from else (dateTo ?: return 0.0)
        
        // Handle same day
        if (from == to) return 1.0
        
        val diff = to.time - from.time
        return (diff / (1000 * 60 * 60 * 24)).toDouble() + 1
    }

    fun getLeaveIcon(type: String) = when (type) {
        "CL" -> Icons.Default.Schedule
        "EL" -> Icons.Default.WbSunny
        "HPL" -> Icons.Default.HealthAndSafety
        "WO" -> Icons.Default.Weekend
        "ML", "PL" -> Icons.Default.ChildCare
        "CCL" -> Icons.Default.ChildFriendly
        "LWA" -> Icons.Default.Block
        "MCL" -> Icons.Default.Favorite
        "CML" -> Icons.Default.MedicalServices
        "LND" -> Icons.Default.Assignment
        else -> Icons.Default.Description
    }

    @Composable
    fun getLeaveColor(type: String): Color {
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        return when (type) {
            "CL" -> if (isDark) Color(0xFF80CBC4) else Color(0xFF009688)
            "EL" -> if (isDark) Color(0xFFFFCC80) else Color(0xFFFF9800)
            "HPL", "CML", "LND" -> if (isDark) Color(0xFFEF9A9A) else Color(0xFFF44336)
            "WO" -> if (isDark) Color(0xFF90CAF9) else Color(0xFF2196F3)
            "MCL" -> if (isDark) Color(0xFFF48FB1) else Color(0xFFE91E63)
            else -> if (isDark) Color(0xFFCE93D8) else Color(0xFF9C27B0)
        }
    }

    val days = calculateDays()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Apply for Leave", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
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
                            leadingIcon = { Icon(getLeaveIcon(type), contentDescription = null, tint = getLeaveColor(type)) },
                            text = { Text(type, fontWeight = FontWeight.Medium) },
                            onClick = {
                                leaveType = type
                                expanded = false
                                isHalfDay = false
                                elEntryType = "taken"
                                hasMedicalCertificate = false
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

            val showToDate = !isHalfDay && leaveType !in listOf("WO", "MCL")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = if (showToDate) Modifier.weight(1f) else Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateFrom?.let { dateFormat.format(it) } ?: "Tap to select",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isHalfDay) "Date" else "From Date") },
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .noRippleClickable {
                                showDatePicker(dateFrom) {
                                    dateFrom = it
                                    if (isHalfDay || leaveType in listOf("WO", "MCL")) dateTo = it
                                }
                            }
                    )
                }

                if (showToDate) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = dateTo?.let { dateFormat.format(it) } ?: "Tap to select",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("To Date") },
                            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .noRippleClickable {
                                    showDatePicker(dateTo ?: dateFrom) { dateTo = it }
                                }
                        )
                    }
                }
            }

            if (days > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    val durationText = if (days % 1.0 == 0.0) days.toInt().toString() else "%.1f".format(days)
                    Text(
                        text = "Duration: $durationText day${if (days != 1.0) "s" else ""}",
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

            if (leaveType in listOf("CML", "LND")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasMedicalCertificate,
                        onCheckedChange = { hasMedicalCertificate = it }
                    )
                    Text(
                        text = "Medical Certificate Provided",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (uiState is LeaveUiState.Error) {
                Text(text = (uiState as LeaveUiState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp))
            }
            
            // Rules Guidance
            val helperText = when (leaveType) {
                "EL" -> "Rule: Max 180 days at a time. Advance credited 15 days on Jan 1 and Jul 1."
                "CL" -> "Rule: Max 7 days at a time. Half days are allowed."
                "HPL" -> "Rule: Can be availed on personal or medical grounds. Advance credited 10 days on Jan 1 and Jul 1."
                "CML" -> "Rule: Commuted Leave on Medical Certificate. Deducts twice the amount of days from HPL balance."
                "LND" -> "Rule: Leave Not Due. Max 360 days allowed during entire service. Requires Medical Certificate."
                "ML" -> "Rule: Max 180 days. Applicable for up to 2 surviving children."
                "PL" -> "Rule: 15 days within 6 months of childbirth, up to 2 children."
                "CCL" -> "Rule: Max 730 days for up to 2 children below 18 years."
                "MCL" -> "Rule: Commuted on Menstrual Grounds (Female only)."
                "LWA" -> "Rule: Leave Without Allowance (Extraordinary Leave)."
                else -> ""
            }

            if (helperText.isNotEmpty()) {
                Text(
                    text = helperText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            Button(
                onClick = {
                    val user = currentUser ?: return@Button
                    val from = dateFrom ?: return@Button
                    val to = if (isHalfDay || leaveType in listOf("WO", "MCL")) from else (dateTo ?: return@Button)
                    val cal = Calendar.getInstance().apply { time = from }
                    val entry = LeaveEntry(
                        kgid = user.kgid,
                        dateFrom = from,
                        dateTo = to,
                        totalDays = days,
                        leaveType = if (leaveType == "MCL") "CL" else leaveType,
                        remark = remark.ifBlank { null },
                        isHalfDay = isHalfDay,
                        isMcl = leaveType == "MCL",
                        elEntryType = if (leaveType == "EL") elEntryType else "taken",
                        year = cal.get(Calendar.YEAR),
                        month = cal.get(Calendar.MONTH) + 1,
                        hasMedicalCertificate = hasMedicalCertificate
                    )
                    leaveViewModel.applyLeave(user, entry)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = dateFrom != null && (isHalfDay || leaveType in listOf("WO", "MCL") || dateTo != null) && uiState !is LeaveUiState.Loading 
                          && (leaveType !in listOf("CML", "LND") || hasMedicalCertificate)
            ) {
                if (uiState is LeaveUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply for Leave")
                    }
                }
            }
        }
    }
}
