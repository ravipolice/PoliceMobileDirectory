package com.example.policemobiledirectory.utils

import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry
import java.util.Calendar

object LeaveBalanceCalculator {

    /**
     * Returns an updated LeaveBalance after deducting for a new LeaveEntry.
     * Does NOT handle persistence; just computes the new state.
     */
    fun applyLeave(balance: LeaveBalance, entry: LeaveEntry): LeaveBalance {
        return when (entry.leaveType.uppercase()) {
            "CL" -> {
                if (entry.isMcl) {
                    // MCL: track separately, does not deduct from CL
                    val cal = Calendar.getInstance()
                    balance.copy(
                        mclUsedThisMonth = balance.mclUsedThisMonth + 1,
                        mclLastUsedMonth = entry.month,
                        mclLastUsedYear = entry.year
                    )
                } else {
                    balance.copy(clRemaining = (balance.clRemaining - entry.totalDays).coerceAtLeast(0.0))
                }
            }
            "MCL" -> {
                balance.copy(
                    mclUsedThisMonth = balance.mclUsedThisMonth + 1,
                    mclLastUsedMonth = entry.month,
                    mclLastUsedYear = entry.year
                )
            }
            "EL" -> {
                // Only deduct if it's a "taken" entry, not "upcoming"
                if (entry.elEntryType == "taken") {
                    balance.copy(elBalance = (balance.elBalance - entry.totalDays).coerceAtLeast(0.0))
                } else {
                    balance // upcoming EL: no balance change
                }
            }
            "HPL" -> balance.copy(hplBalance = (balance.hplBalance - entry.totalDays).coerceAtLeast(0.0))
            "CCL" -> balance.copy(cclUsed = balance.cclUsed + entry.totalDays)
            "ML" -> balance.copy(maternityUsedCount = balance.maternityUsedCount + 1)
            "PL" -> balance.copy(paternityUsedCount = balance.paternityUsedCount + 1)
            else -> balance // WO, LWA don't deduct from fixed balances
        }
    }

    /**
     * Reverses a leave entry — used when editing or deleting an entry.
     * Returns the balance as if the entry was never applied.
     */
    fun reverseLeave(balance: LeaveBalance, entry: LeaveEntry): LeaveBalance {
        return when (entry.leaveType.uppercase()) {
            "CL" -> {
                if (entry.isMcl) {
                    balance.copy(
                        mclUsedThisMonth = (balance.mclUsedThisMonth - 1).coerceAtLeast(0)
                    )
                } else {
                    val newRemaining = (balance.clRemaining + entry.totalDays)
                        .coerceAtMost(balance.clAnnualLimit.toDouble())
                    balance.copy(clRemaining = newRemaining)
                }
            }
            "MCL" -> {
                balance.copy(
                    mclUsedThisMonth = (balance.mclUsedThisMonth - 1).coerceAtLeast(0)
                )
            }
            "EL" -> {
                if (entry.elEntryType == "taken") {
                    balance.copy(elBalance = balance.elBalance + entry.totalDays)
                } else {
                    balance
                }
            }
            "HPL" -> balance.copy(hplBalance = balance.hplBalance + entry.totalDays)
            "CCL" -> balance.copy(cclUsed = (balance.cclUsed - entry.totalDays).coerceAtLeast(0.0))
            "ML" -> balance.copy(maternityUsedCount = (balance.maternityUsedCount - 1).coerceAtLeast(0))
            "PL" -> balance.copy(paternityUsedCount = (balance.paternityUsedCount - 1).coerceAtLeast(0))
            else -> balance
        }
    }

    /**
     * Updates EL manual balance (carry-forward). Recalculates elBalance
     * based on new manual balance minus total EL taken.
     */
    fun updateElManualBalance(balance: LeaveBalance, newManualBalance: Double, totalElTaken: Double): LeaveBalance {
        val newElBalance = (newManualBalance - totalElTaken).coerceAtLeast(0.0)
        return balance.copy(
            elManualBalance = newManualBalance,
            elBalance = newElBalance
        )
    }

    /**
     * Updates HPL manual balance (starting balance). Recalculates hplBalance
     * based on new manual balance minus total HPL taken.
     */
    fun updateHplManualBalance(balance: LeaveBalance, newManualBalance: Double, totalHplTaken: Double): LeaveBalance {
        val newHplBalance = (newManualBalance - totalHplTaken).coerceAtLeast(0.0)
        return balance.copy(
            hplManualBalance = newManualBalance,
            hplBalance = newHplBalance
        )
    }

    /**
     * Updates CL annual limit (10 or 15). Recalculates clRemaining.
     */
    fun updateClLimit(balance: LeaveBalance, newLimit: Int, totalClTaken: Double): LeaveBalance {
        val newRemaining = (newLimit.toDouble() - totalClTaken).coerceAtLeast(0.0)
        return balance.copy(
            clAnnualLimit = newLimit,
            clRemaining = newRemaining
        )
    }
}
