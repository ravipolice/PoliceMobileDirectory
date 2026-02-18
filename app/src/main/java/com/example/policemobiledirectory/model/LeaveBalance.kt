package com.example.policemobiledirectory.model

data class LeaveBalance(
    val kgid: String = "",
    val clYear: Int = 0,
    val clAnnualLimit: Int = 15,        // 10 or 15 — user-configurable
    val clRemaining: Double = 15.0,     // Double for half-day support
    val elManualBalance: Double = 0.0,  // User-entered carry-forward EL balance
    val elBalance: Double = 0.0,        // Current EL remaining (elManualBalance - taken)
    val hplBalance: Double = 0.0,       // Double for consistency
    val cclUsed: Double = 0.0,          // Double for consistency
    val maternityUsedCount: Int = 0,
    val paternityUsedCount: Int = 0,
    val mclUsedThisMonth: Int = 0,      // Resets each month; max 1/month for females
    val mclLastUsedMonth: Int = 0,      // Month of last MCL use (for reset tracking)
    val mclLastUsedYear: Int = 0,       // Year of last MCL use
    val lastResetYear: Int = 0,
    val lastCreditDate: String = ""     // Format: YYYY-MM-DD
)
