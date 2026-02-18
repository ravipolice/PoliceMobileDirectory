package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry

object LeaveValidationEngine {

    /**
     * Validates a leave application against KCSR rules and user balance.
     * Returns Result.success(Unit) if valid, or Result.failure with descriptive message.
     */
    fun validateLeave(
        employee: Employee,
        balance: LeaveBalance,
        entry: LeaveEntry,
        woCountThisMonth: Int
    ): Result<Unit> {

        // 1. Gender Rules
        val genderError = checkGenderCompatibility(employee.gender, entry.leaveType, entry.isMcl)
        if (genderError != null) return Result.failure(Exception(genderError))

        // 2. Leave Specific Rules
        when (entry.leaveType.uppercase()) {
            "CL" -> {
                if (entry.isMcl) {
                    // MCL rules
                    if (employee.gender.lowercase() != "female") {
                        return Result.failure(Exception("MCL is only available for female employees"))
                    }
                    if (balance.mclLastUsedMonth == entry.month && balance.mclLastUsedYear == entry.year) {
                        return Result.failure(Exception("MCL already availed this month"))
                    }
                } else {
                    // Regular CL rules
                    val maxDays = if (entry.isHalfDay) 0.5 else 7.0
                    if (entry.totalDays > maxDays && !entry.isHalfDay) {
                        return Result.failure(Exception("Casual Leave cannot exceed 7 days at a time"))
                    }
                    if (entry.isHalfDay && entry.totalDays != 0.5) {
                        return Result.failure(Exception("Half-day CL must be exactly 0.5 days"))
                    }
                    if (entry.totalDays > balance.clRemaining) {
                        return Result.failure(Exception("Insufficient CL balance (${balance.clRemaining} days remaining)"))
                    }
                }
            }
            "MCL" -> {
                if (employee.gender.lowercase() != "female") {
                    return Result.failure(Exception("MCL is only available for female employees"))
                }
                if (balance.mclLastUsedMonth == entry.month && balance.mclLastUsedYear == entry.year) {
                    return Result.failure(Exception("MCL already availed this month"))
                }
            }
            "EL" -> {
                if (entry.elEntryType == "taken") {
                    if (entry.totalDays > balance.elBalance) {
                        return Result.failure(Exception("Insufficient EL balance (${balance.elBalance} days remaining)"))
                    }
                }
                // "upcoming" EL: no balance check needed
            }
            "HPL" -> {
                if (entry.totalDays > balance.hplBalance) {
                    return Result.failure(Exception("Insufficient HPL balance (${balance.hplBalance} days remaining)"))
                }
            }
            "WO" -> {
                if (woCountThisMonth >= 4) {
                    return Result.failure(Exception("Monthly Weekly Off limit reached (max 4 per month)"))
                }
            }
            "CCL" -> {
                if (entry.totalDays + balance.cclUsed > 730) {
                    return Result.failure(Exception("Insufficient CCL balance (Max 730 days total)"))
                }
            }
            "ML" -> {
                if (balance.maternityUsedCount >= 2) {
                    return Result.failure(Exception("Maternity leave already used for two children"))
                }
            }
            "PL" -> {
                if (balance.paternityUsedCount >= 2) {
                    return Result.failure(Exception("Paternity leave already used for two children"))
                }
            }
        }

        return Result.success(Unit)
    }

    private fun checkGenderCompatibility(gender: String, leaveType: String, isMcl: Boolean): String? {
        val g = gender.lowercase()
        val type = leaveType.uppercase()

        return when {
            isMcl || type == "MCL" -> if (g != "female") "MCL is only available for female employees" else null
            type == "ML" || type == "CCL" -> if (g != "female") "Leave type not allowed for selected gender" else null
            type == "PL" -> if (g != "male") "Leave type not allowed for selected gender" else null
            else -> null
        }
    }
}
