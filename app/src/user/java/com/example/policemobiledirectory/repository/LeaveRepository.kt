package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.model.LeaveCreditLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "LeaveRepository"
    private val leaveCollection = firestore.collection("leaveBalances")

    private fun getEntriesCollection(kgid: String) =
        leaveCollection.document(kgid).collection("entries")

    private fun getLogsCollection(kgid: String) =
        leaveCollection.document(kgid).collection("logs")

    suspend fun getLeaveBalance(kgid: String): LeaveBalance? {
        return try {
            Log.d(TAG, "Fetching leave balance for KGID: $kgid")
            val doc = leaveCollection.document(kgid).get().await()
            if (doc.exists()) {
                Log.d(TAG, "Leave balance found for KGID: $kgid")
                doc.toObject(LeaveBalance::class.java)
            } else {
                Log.d(TAG, "No leave balance found, creating new for KGID: $kgid")
                val newBalance = LeaveBalance(kgid = kgid)
                leaveCollection.document(kgid).set(newBalance).await()
                Log.d(TAG, "New leave balance created for KGID: $kgid")
                newBalance
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leave balance for KGID: $kgid", e)
            null
        }
    }

    suspend fun saveLeaveBalance(balance: LeaveBalance) {
        try {
            leaveCollection.document(balance.kgid).set(balance).await()
            Log.d(TAG, "Leave balance saved for KGID: ${balance.kgid}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving leave balance", e)
            throw e
        }
    }

    suspend fun saveLeaveEntry(balance: LeaveBalance, entry: LeaveEntry) {
        val kgid = balance.kgid
        Log.d(TAG, "Saving leave entry for KGID: $kgid, type=${entry.leaveType}, days=${entry.totalDays}")
        try {
            firestore.runTransaction { transaction ->
                val balanceRef = leaveCollection.document(kgid)
                val entryRef = if (entry.id.isEmpty()) {
                    getEntriesCollection(kgid).document()
                } else {
                    getEntriesCollection(kgid).document(entry.id)
                }
                val finalEntry = entry.copy(id = entryRef.id)
                transaction.set(balanceRef, balance)
                transaction.set(entryRef, finalEntry)
            }.await()
            Log.d(TAG, "Leave entry saved successfully for KGID: $kgid")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving leave entry for KGID: $kgid", e)
            throw e
        }
    }

    suspend fun updateLeaveEntry(balance: LeaveBalance, entry: LeaveEntry) {
        val kgid = balance.kgid
        Log.d(TAG, "Updating leave entry ${entry.id} for KGID: $kgid")
        try {
            firestore.runTransaction { transaction ->
                val balanceRef = leaveCollection.document(kgid)
                val entryRef = getEntriesCollection(kgid).document(entry.id)
                transaction.set(balanceRef, balance)
                transaction.set(entryRef, entry)
            }.await()
            Log.d(TAG, "Leave entry updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating leave entry", e)
            throw e
        }
    }

    suspend fun deleteLeaveEntry(balance: LeaveBalance, entry: LeaveEntry) {
        val kgid = balance.kgid
        Log.d(TAG, "Deleting leave entry ${entry.id} for KGID: $kgid")
        try {
            firestore.runTransaction { transaction ->
                val balanceRef = leaveCollection.document(kgid)
                val entryRef = getEntriesCollection(kgid).document(entry.id)
                transaction.set(balanceRef, balance)
                transaction.delete(entryRef)
            }.await()
            Log.d(TAG, "Leave entry deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting leave entry", e)
            throw e
        }
    }

    suspend fun saveCreditUpdate(balance: LeaveBalance, logs: List<LeaveCreditLog>) {
        val kgid = balance.kgid
        firestore.runTransaction { transaction ->
            val balanceRef = leaveCollection.document(kgid)
            transaction.set(balanceRef, balance)
            logs.forEach { log ->
                val logRef = getLogsCollection(kgid).document()
                transaction.set(logRef, log.copy(id = logRef.id))
            }
        }.await()
    }

    fun getLeaveEntries(kgid: String): Flow<List<LeaveEntry>> = flow {
        try {
            val snapshot = getEntriesCollection(kgid)
                .orderBy("dateFrom", Query.Direction.DESCENDING)
                .get()
                .await()
            emit(snapshot.toObjects(LeaveEntry::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leave entries", e)
            emit(emptyList())
        }
    }

    fun getEntriesByType(kgid: String, leaveType: String): Flow<List<LeaveEntry>> = flow {
        try {
            val snapshot = getEntriesCollection(kgid)
                .whereEqualTo("leaveType", leaveType)
                .orderBy("dateFrom", Query.Direction.DESCENDING)
                .get()
                .await()
            emit(snapshot.toObjects(LeaveEntry::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching $leaveType entries", e)
            emit(emptyList())
        }
    }

    fun getMclEntries(kgid: String): Flow<List<LeaveEntry>> = flow {
        try {
            val snapshot = getEntriesCollection(kgid)
                .whereEqualTo("isMcl", true)
                .orderBy("dateFrom", Query.Direction.DESCENDING)
                .get()
                .await()
            emit(snapshot.toObjects(LeaveEntry::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MCL entries", e)
            emit(emptyList())
        }
    }

    suspend fun getWoCountForMonth(kgid: String, year: Int, month: Int): Int {
        return try {
            val snapshot = getEntriesCollection(kgid)
                .whereEqualTo("leaveType", "WO")
                .whereEqualTo("year", year)
                .whereEqualTo("month", month)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching WO count", e)
            0
        }
    }

    suspend fun getLeaveStatistics(kgid: String, year: Int): com.example.policemobiledirectory.model.LeaveStatistics? {
        return try {
            val balance = getLeaveBalance(kgid) ?: return null
            val snapshot = getEntriesCollection(kgid)
                .whereEqualTo("year", year)
                .get()
                .await()

            val entries = snapshot.toObjects(LeaveEntry::class.java)

            val totalTaken = entries.filter { it.elEntryType != "upcoming" }.sumOf { it.totalDays }.toInt()
            val totalRemaining = (balance.clRemaining + balance.elBalance + balance.hplBalance).toInt()

            val typeBreakdown = entries
                .filter { it.elEntryType != "upcoming" }
                .groupBy { it.leaveType }
                .mapValues { (_, list) -> list.sumOf { it.totalDays }.toInt() }

            val mostUsedType = typeBreakdown.maxByOrNull { it.value }?.key ?: ""

            val monthlyBreakdown = entries
                .filter { it.elEntryType != "upcoming" }
                .groupBy { it.month }
                .mapValues { (_, list) -> list.sumOf { it.totalDays }.toInt() }

            val averagePerMonth = if (entries.isNotEmpty()) {
                totalTaken.toDouble() / monthlyBreakdown.keys.size.coerceAtLeast(1)
            } else 0.0

            val totalAvailable = balance.clAnnualLimit + balance.elBalance + balance.hplBalance
            val utilizationPercentage = if (totalAvailable > 0) {
                (totalTaken.toFloat() / totalAvailable.toFloat()) * 100f
            } else 0f

            com.example.policemobiledirectory.model.LeaveStatistics(
                totalTaken = totalTaken,
                totalRemaining = totalRemaining,
                mostUsedType = mostUsedType,
                averagePerMonth = averagePerMonth,
                utilizationPercentage = utilizationPercentage,
                monthlyBreakdown = monthlyBreakdown,
                leaveTypeBreakdown = typeBreakdown
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating leave statistics", e)
            null
        }
    }

    suspend fun getMonthlyLeaveData(kgid: String, year: Int): Map<Int, List<LeaveEntry>> {
        return try {
            val snapshot = getEntriesCollection(kgid)
                .whereEqualTo("year", year)
                .get()
                .await()
            snapshot.toObjects(LeaveEntry::class.java).groupBy { it.month }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching monthly leave data", e)
            emptyMap()
        }
    }
}
