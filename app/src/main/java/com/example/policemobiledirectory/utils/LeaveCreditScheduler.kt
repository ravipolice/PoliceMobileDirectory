package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveCreditLog
import java.text.SimpleDateFormat
import java.util.*

object LeaveCreditScheduler {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Checks if credits are due and returns an updated LeaveBalance and a list of logs.
     * Returns null if no updates are needed.
     */
    fun processCredits(balance: LeaveBalance, currentDate: Date = Date()): Pair<LeaveBalance, List<LeaveCreditLog>>? {
        val cal = Calendar.getInstance()
        cal.time = currentDate
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) // 0-indexed (Jan is 0)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        val newLogs = mutableListOf<LeaveCreditLog>()
        var updatedBalance = balance

        // 1. Annual CL Reset (Jan 1)
        if (currentMonth == Calendar.JANUARY && currentDay >= 1 && balance.clYear != currentYear) {
            updatedBalance = updatedBalance.copy(
                clYear = currentYear,
                clRemaining = updatedBalance.clAnnualLimit.toDouble()
            )
            newLogs.add(LeaveCreditLog(
                kgid = balance.kgid,
                type = "CL_RESET",
                amount = updatedBalance.clAnnualLimit,
                year = currentYear,
                date = currentDate
            ))
        }

        // 2. Periodic Credits (Jan 1 and Jul 1)
        val todayStr = dateFormat.format(currentDate)
        if (balance.lastCreditDate != todayStr) {
            
            // Check if Jan 1 credit is needed
            if (currentMonth >= Calendar.JANUARY && isCreditNeededForCycle(balance.lastCreditDate, currentYear, Calendar.JANUARY)) {
                 updatedBalance = updatedBalance.copy(
                    elBalance = (updatedBalance.elBalance + 15.0).coerceAtMost(300.0),
                    hplBalance = updatedBalance.hplBalance + 10.0,
                    lastCreditDate = todayStr
                )
                newLogs.add(LeaveCreditLog(kgid = balance.kgid, type = "EL_CREDIT", amount = 15, year = currentYear, date = currentDate))
                newLogs.add(LeaveCreditLog(kgid = balance.kgid, type = "HPL_CREDIT", amount = 10, year = currentYear, date = currentDate))
            }
            
            // Check if Jul 1 credit is needed
            if (currentMonth >= Calendar.JULY && isCreditNeededForCycle(balance.lastCreditDate, currentYear, Calendar.JULY)) {
                updatedBalance = updatedBalance.copy(
                    elBalance = (updatedBalance.elBalance + 15.0).coerceAtMost(300.0),
                    hplBalance = updatedBalance.hplBalance + 10.0,
                    lastCreditDate = todayStr
                )
                newLogs.add(LeaveCreditLog(kgid = balance.kgid, type = "EL_CREDIT", amount = 15, year = currentYear, date = currentDate))
                newLogs.add(LeaveCreditLog(kgid = balance.kgid, type = "HPL_CREDIT", amount = 10, year = currentYear, date = currentDate))
            }
        }

        return if (newLogs.isNotEmpty()) {
            updatedBalance to newLogs
        } else {
            null
        }
    }

    private fun isCreditNeededForCycle(lastCreditDate: String, currentYear: Int, cycleMonth: Int): Boolean {
        if (lastCreditDate.isEmpty()) return true
        
        val lastDate = try { dateFormat.parse(lastCreditDate) } catch (e: Exception) { null } ?: return true
        val lastCal = Calendar.getInstance()
        lastCal.time = lastDate
        
        val lastYear = lastCal.get(Calendar.YEAR)
        val lastMonth = lastCal.get(Calendar.MONTH)
        
        return if (lastYear < currentYear) {
            true
        } else {
            lastMonth < cycleMonth
        }
    }
}
