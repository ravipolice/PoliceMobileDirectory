package com.example.policemobiledirectory.model

data class LeaveBalance(
    val kgid: String = "",
    val clYear: Int = 0,
    val clAnnualLimit: Int = 15,
    val clRemaining: Double = 15.0,
    val elManualBalance: Double = 0.0,
    val elBalance: Double = 0.0,
    val hplManualBalance: Double = 0.0,
    val hplBalance: Double = 0.0,
    val cclUsed: Double = 0.0,
    val maternityUsedCount: Int = 0,
    val paternityUsedCount: Int = 0,
    val mclUsedThisMonth: Int = 0,
    val woUsedThisMonth: Int = 0,
    val mclLastUsedMonth: Int = 0,
    val mclLastUsedYear: Int = 0,
    val lastResetYear: Int = 0,
    val lastCreditDate: String = "",
    val lastElHplCreditDate: String = ""
)
