package com.example.policemobiledirectory.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_balances WHERE kgid = :kgid")
    suspend fun getBalance(kgid: String): LeaveBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: LeaveBalanceEntity)

    @Query("SELECT * FROM leave_entries WHERE kgid = :kgid ORDER BY dateFrom DESC")
    fun getAllEntries(kgid: String): Flow<List<LeaveEntryEntity>>

    @Query("SELECT * FROM leave_entries WHERE kgid = :kgid AND year = :year AND elEntryType != 'upcoming' ORDER BY dateFrom DESC")
    suspend fun getEntriesForStats(kgid: String, year: Int): List<LeaveEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaveEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LeaveEntryEntity>)

    @Update
    suspend fun updateEntry(entry: LeaveEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: LeaveEntryEntity)

    @Query("DELETE FROM leave_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: String)

    @Query("DELETE FROM leave_entries WHERE kgid = :kgid")
    suspend fun deleteAllEntries(kgid: String)

    @Query("DELETE FROM leave_credit_logs WHERE kgid = :kgid")
    suspend fun deleteAllCreditLogs(kgid: String)

    @Transaction
    suspend fun restoreAllData(balance: LeaveBalanceEntity, entries: List<LeaveEntryEntity>, logs: List<LeaveCreditLogEntity>) {
        deleteAllEntries(balance.kgid)
        deleteAllCreditLogs(balance.kgid)
        insertBalance(balance)
        if (entries.isNotEmpty()) {
            insertEntries(entries)
        }
        if (logs.isNotEmpty()) {
            insertCreditLogs(logs)
        }
    }

    @Query("SELECT * FROM leave_entries WHERE kgid = :kgid AND leaveType = 'WO' AND year = :year AND month = :month")
    suspend fun getWoEntries(kgid: String, year: Int, month: Int): List<LeaveEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditLogs(logs: List<LeaveCreditLogEntity>)

    @Query("SELECT * FROM leave_credit_logs WHERE kgid = :kgid ORDER BY date DESC")
    fun getCreditLogs(kgid: String): Flow<List<LeaveCreditLogEntity>>

    @Transaction
    suspend fun saveEntryWithBalance(balance: LeaveBalanceEntity, entry: LeaveEntryEntity) {
        insertBalance(balance)
        if (entry.localId == 0L) {
            insertEntry(entry)
        } else {
            updateEntry(entry)
        }
    }

    @Transaction
    suspend fun deleteEntryWithBalance(balance: LeaveBalanceEntity, entry: LeaveEntryEntity) {
        insertBalance(balance)
        if (entry.localId != 0L) {
            deleteEntry(entry)
        } else if (entry.id.isNotEmpty()) {
            deleteEntryById(entry.id)
        }
    }
}
