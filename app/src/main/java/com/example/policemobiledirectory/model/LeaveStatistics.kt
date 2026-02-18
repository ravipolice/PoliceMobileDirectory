package com.example.policemobiledirectory.model

data class LeaveStatistics(
    val totalTaken: Int = 0,
    val totalRemaining: Int = 0,
    val mostUsedType: String = "",
    val averagePerMonth: Double = 0.0,
    val utilizationPercentage: Float = 0f,
    val monthlyBreakdown: Map<Int, Int> = emptyMap(),
    val leaveTypeBreakdown: Map<String, Int> = emptyMap()
)
