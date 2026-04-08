package com.example.policemobiledirectory.repository

import android.util.Log
import com.example.policemobiledirectory.data.local.*
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.model.LeaveCreditLog
import com.example.policemobiledirectory.model.LeaveStatistics
import com.example.policemobiledirectory.utils.GoogleDriveSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaveRepository @Inject constructor(
    private val leaveDao: LeaveDao,
    private val driveSyncManager: GoogleDriveSyncManager
) {
    private val TAG = "LeaveRepository"

    suspend fun getLeaveBalance(kgid: String): LeaveBalance? {
        if (kgid.isBlank()) return null
        return try {
            Log.d(TAG, "Fetching local leave balance for KGID: $kgid")
            val localBalance = leaveDao.getBalance(kgid)
            if (localBalance != null) {
                localBalance.toDomain()
            } else {
                // Try restore from Google Drive if local is empty
                Log.d(TAG, "Local balance empty, attempting GDrive restore for KGID: $kgid")
                val result = driveSyncManager.downloadBackup()
                if (result is GoogleDriveSyncManager.SyncResult.Success && result.data?.balance?.kgid == kgid) {
                    Log.d(TAG, "Successfully restored from GDrive")
                    restoreFromBackup(result.data!!)
                    result.data.balance
                } else {
                    Log.d(TAG, "No backup found or error, creating new balance")
                    val newBalance = LeaveBalance(kgid = kgid)
                    saveLeaveBalance(newBalance)
                    newBalance
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting leave balance", e)
            null
        }
    }

    suspend fun saveLeaveBalance(balance: LeaveBalance) {
        try {
            leaveDao.insertBalance(balance.toEntity())
            triggerSync(balance.kgid)
            Log.d(TAG, "Leave balance saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving leave balance", e)
            throw e
        }
    }

    suspend fun saveLeaveEntry(balance: LeaveBalance, entry: LeaveEntry) {
        try {
            leaveDao.saveEntryWithBalance(balance.toEntity(), entry.toEntity())
            triggerSync(balance.kgid)
            Log.d(TAG, "Leave entry and balance saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving leave entry", e)
            throw e
        }
    }

    suspend fun updateLeaveEntry(balance: LeaveBalance, entry: LeaveEntry) {
        try {
            leaveDao.saveEntryWithBalance(balance.toEntity(), entry.toEntity())
            triggerSync(balance.kgid)
            Log.d(TAG, "Leave entry updated locally")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating leave entry", e)
            throw e
        }
    }

    suspend fun deleteLeaveEntry(balance: LeaveBalance, entry: LeaveEntry) {
        try {
            leaveDao.deleteEntryWithBalance(balance.toEntity(), entry.toEntity())
            triggerSync(balance.kgid)
            Log.d(TAG, "Leave entry deleted locally")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting leave entry", e)
            throw e
        }
    }

    suspend fun saveCreditUpdate(balance: LeaveBalance, logs: List<LeaveCreditLog>) {
        try {
            leaveDao.insertBalance(balance.toEntity())
            leaveDao.insertCreditLogs(logs.map { it.toEntity() })
            triggerSync(balance.kgid)
            Log.d(TAG, "Credit update and logs saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving credit update", e)
            throw e
        }
    }

    fun getCreditLogs(kgid: String): Flow<List<LeaveCreditLog>> {
        return leaveDao.getCreditLogs(kgid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getLeaveEntries(kgid: String): Flow<List<LeaveEntry>> {
        return leaveDao.getAllEntries(kgid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getWoCountForMonth(kgid: String, year: Int, month: Int): Int {
        return try {
            leaveDao.getWoEntries(kgid, year, month).size
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching WO count", e)
            0
        }
    }

    suspend fun manualBackup(kgid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting manual backup for KGID: $kgid")
            val balance = leaveDao.getBalance(kgid)?.toDomain()
            val entries = leaveDao.getAllEntries(kgid).first().map { it.toDomain() }
            val logs = leaveDao.getCreditLogs(kgid).first().map { it.toDomain() }
            
            val result = driveSyncManager.uploadBackup(balance, entries, logs)
            val success = result is GoogleDriveSyncManager.SyncResult.Success
            if (success) {
                Log.d(TAG, "Manual backup successful for KGID: $kgid")
            } else {
                Log.e(TAG, "Manual backup failed: $result")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Manual backup failed with exception", e)
            false
        }
    }

    suspend fun manualRestore(kgid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting manual restore for KGID: $kgid")
            val result = driveSyncManager.downloadBackup()
            if (result is GoogleDriveSyncManager.SyncResult.Success && result.data?.balance?.kgid == kgid) {
                restoreFromBackup(result.data!!)
                Log.d(TAG, "Manual restore successful for KGID: $kgid")
                true
            } else {
                Log.w(TAG, "Manual restore failed: result is $result")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manual restore failed with exception", e)
            false
        }
    }

    private suspend fun triggerSync(kgid: String, retryCount: Int = 1) {
        withContext(Dispatchers.IO) {
            try {
                val balance = leaveDao.getBalance(kgid)?.toDomain()
                val entries = leaveDao.getAllEntries(kgid).first().map { it.toDomain() }
                val logs = leaveDao.getCreditLogs(kgid).first().map { it.toDomain() }
                
                val result = driveSyncManager.uploadBackup(balance, entries, logs)
                val success = result is GoogleDriveSyncManager.SyncResult.Success
                if (!success && retryCount > 0) {
                    Log.w(TAG, "Background sync failed for $kgid, retrying ($retryCount left)...")
                    kotlinx.coroutines.delay(3000)
                    triggerSync(kgid, retryCount - 1)
                } else if (!success) {
                    Log.e(TAG, "Background sync failed for $kgid after retries")
                } else {
                    Log.d(TAG, "Background sync successful for $kgid")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background sync exception for $kgid: ${e.message}")
                if (retryCount > 0) {
                    kotlinx.coroutines.delay(3000)
                    triggerSync(kgid, retryCount - 1)
                }
            }
        }
    }

    private suspend fun restoreFromBackup(backup: GoogleDriveSyncManager.LeaveBackupData) {
        backup.balance?.let { balance ->
            leaveDao.restoreAllData(
                balance.toEntity(),
                backup.entries.map { it.toEntity() },
                backup.creditLogs.map { it.toEntity() }
            )
            Log.d(TAG, "Restore from backup completed for KGID: ${balance.kgid}")
        }
    }

    suspend fun getLeaveStatistics(kgid: String, year: Int): LeaveStatistics? = withContext(Dispatchers.IO) {
        if (kgid.isBlank()) return@withContext null
        try {
            val balance = getLeaveBalance(kgid) ?: return@withContext null
            val entries = leaveDao.getEntriesForStats(kgid, year)
                .map { it.toDomain() }

            val totalTaken = entries.sumOf { it.totalDays }
            val totalRemaining = balance.clRemaining + balance.elBalance + balance.hplBalance

            val leaveTypeBreakdown = entries
                .groupBy { it.leaveType }
                .mapValues { (_, list) -> list.sumOf { it.totalDays } }

            val mostUsedType = leaveTypeBreakdown.maxByOrNull { it.value }?.key ?: ""

            val monthlyBreakdown = entries
                .groupBy { it.month }
                .mapValues { (_, list) -> list.sumOf { it.totalDays } }

            val averagePerMonth = if (monthlyBreakdown.isNotEmpty()) {
                totalTaken / monthlyBreakdown.size
            } else 0.0

            val totalAvailable = (balance.clAnnualLimit + balance.elBalance + balance.hplBalance)
            val utilizationPercentage = if (totalAvailable > 0) {
                (totalTaken.toFloat() / totalAvailable.toFloat()) * 100f
            } else 0f

            LeaveStatistics(
                kgid = kgid,
                year = year,
                totalTaken = totalTaken,
                totalRemaining = totalRemaining,
                mostUsedType = mostUsedType,
                averagePerMonth = averagePerMonth,
                utilizationPercentage = utilizationPercentage,
                monthlyBreakdown = monthlyBreakdown,
                leaveTypeBreakdown = leaveTypeBreakdown
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating leave statistics", e)
            null
        }
    }

    fun hasDrivePermission(): Boolean {
        return driveSyncManager.hasDrivePermission()
    }
}
