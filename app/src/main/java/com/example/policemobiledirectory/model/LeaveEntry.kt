package com.example.policemobiledirectory.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LeaveEntry(
    val id: String = "",
    val kgid: String = "",
    val dateFrom: Date? = null,
    val dateTo: Date? = null,
    val totalDays: Double = 0.0,       // Double to support half-day (0.5)
    val leaveType: String = "",         // CL, EL, HPL, WO, EOL, ML, PL, CCL, MCL
    val remark: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val year: Int = 0,
    val month: Int = 0,
    val isHalfDay: Boolean = false,     // true if CL half-day (0.5 days)
    val isMcl: Boolean = false,         // true if Menstrual CL (MCL)
    val elEntryType: String = "taken"   // "taken" | "upcoming" — for EL entries
)
