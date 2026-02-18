package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.model.LeaveBalance
import com.example.policemobiledirectory.model.LeaveEntry
import com.example.policemobiledirectory.repository.LeaveRepository
import com.example.policemobiledirectory.utils.LeaveBalanceCalculator
import com.example.policemobiledirectory.utils.LeaveCreditScheduler
import com.example.policemobiledirectory.utils.LeaveValidationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val leaveRepository: LeaveRepository
) : ViewModel() {

    companion object {
        private const val TAG = "LeaveViewModel"
    }

    private val _balance = MutableStateFlow<LeaveBalance?>(null)
    val balance: StateFlow<LeaveBalance?> = _balance.asStateFlow()

    private val _entries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val entries: StateFlow<List<LeaveEntry>> = _entries.asStateFlow()

    private val _uiState = MutableStateFlow<LeaveUiState>(LeaveUiState.Idle)
    val uiState: StateFlow<LeaveUiState> = _uiState.asStateFlow()

    private val _statistics = MutableStateFlow<com.example.policemobiledirectory.model.LeaveStatistics?>(null)
    val statistics: StateFlow<com.example.policemobiledirectory.model.LeaveStatistics?> = _statistics.asStateFlow()

    // Per-type entry flows
    private val _clEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val clEntries: StateFlow<List<LeaveEntry>> = _clEntries.asStateFlow()

    private val _elEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val elEntries: StateFlow<List<LeaveEntry>> = _elEntries.asStateFlow()

    private val _hplEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val hplEntries: StateFlow<List<LeaveEntry>> = _hplEntries.asStateFlow()

    private val _woEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val woEntries: StateFlow<List<LeaveEntry>> = _woEntries.asStateFlow()

    private val _cclEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val cclEntries: StateFlow<List<LeaveEntry>> = _cclEntries.asStateFlow()

    private val _mclEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val mclEntries: StateFlow<List<LeaveEntry>> = _mclEntries.asStateFlow()

    private val _otherEntries = MutableStateFlow<List<LeaveEntry>>(emptyList())
    val otherEntries: StateFlow<List<LeaveEntry>> = _otherEntries.asStateFlow()

    fun refreshData(employee: Employee) {
        viewModelScope.launch {
            _uiState.value = LeaveUiState.Loading
            try {
                val currentBalance = leaveRepository.getLeaveBalance(employee.kgid)
                if (currentBalance != null) {
                    val creditUpdate = LeaveCreditScheduler.processCredits(currentBalance)
                    if (creditUpdate != null) {
                        val (updatedBalance, logs) = creditUpdate
                        leaveRepository.saveCreditUpdate(updatedBalance, logs)
                        _balance.value = updatedBalance
                    } else {
                        _balance.value = currentBalance
                    }

                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val stats = leaveRepository.getLeaveStatistics(employee.kgid, currentYear)
                    _statistics.value = stats

                    leaveRepository.getLeaveEntries(employee.kgid).collect { all ->
                        _entries.value = all
                        _clEntries.value = all.filter { it.leaveType == "CL" && !it.isMcl }
                        _elEntries.value = all.filter { it.leaveType == "EL" }
                        _hplEntries.value = all.filter { it.leaveType == "HPL" }
                        _woEntries.value = all.filter { it.leaveType == "WO" }
                        _cclEntries.value = all.filter { it.leaveType == "CCL" }
                        _mclEntries.value = all.filter { it.isMcl || it.leaveType == "MCL" }
                        _otherEntries.value = all.filter { it.leaveType in listOf("ML", "PL", "LWA") }
                        _uiState.value = LeaveUiState.Success
                    }
                } else {
                    _uiState.value = LeaveUiState.Error("Failed to fetch leave balance")
                }
            } catch (e: Exception) {
                _uiState.value = LeaveUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun applyLeave(employee: Employee, entry: LeaveEntry) {
        viewModelScope.launch {
            try {
                val currentBalance = _balance.value
                if (currentBalance == null) {
                    _uiState.value = LeaveUiState.Error("Leave balance not loaded. Please refresh and try again.")
                    return@launch
                }

                Log.d(TAG, "Applying leave for ${employee.kgid}: type=${entry.leaveType}, days=${entry.totalDays}")
                _uiState.value = LeaveUiState.Loading

                val woCount = leaveRepository.getWoCountForMonth(employee.kgid, entry.year, entry.month)
                val validation = LeaveValidationEngine.validateLeave(employee, currentBalance, entry, woCount)

                if (validation.isSuccess) {
                    val updatedBalance = LeaveBalanceCalculator.applyLeave(currentBalance, entry)
                    leaveRepository.saveLeaveEntry(updatedBalance, entry)
                    _balance.value = updatedBalance
                    refreshData(employee)
                    _uiState.value = LeaveUiState.Success
                } else {
                    val errorMsg = validation.exceptionOrNull()?.message ?: "Validation failed"
                    Log.e(TAG, "Leave validation failed: $errorMsg")
                    _uiState.value = LeaveUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying leave", e)
                _uiState.value = LeaveUiState.Error("Failed to save leave: ${e.message}")
            }
        }
    }

    fun deleteLeaveEntry(employee: Employee, entry: LeaveEntry) {
        viewModelScope.launch {
            try {
                val currentBalance = _balance.value
                if (currentBalance == null) {
                    _uiState.value = LeaveUiState.Error("Leave balance not loaded.")
                    return@launch
                }

                Log.d(TAG, "Deleting leave entry ${entry.id}")
                _uiState.value = LeaveUiState.Loading

                // Reverse the balance impact of this entry
                val restoredBalance = LeaveBalanceCalculator.reverseLeave(currentBalance, entry)
                leaveRepository.deleteLeaveEntry(restoredBalance, entry)
                _balance.value = restoredBalance
                refreshData(employee)
                _uiState.value = LeaveUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting leave entry", e)
                _uiState.value = LeaveUiState.Error("Failed to delete: ${e.message}")
            }
        }
    }

    fun updateLeaveEntry(employee: Employee, oldEntry: LeaveEntry, newEntry: LeaveEntry) {
        viewModelScope.launch {
            try {
                val currentBalance = _balance.value
                if (currentBalance == null) {
                    _uiState.value = LeaveUiState.Error("Leave balance not loaded.")
                    return@launch
                }

                Log.d(TAG, "Updating leave entry ${oldEntry.id}")
                _uiState.value = LeaveUiState.Loading

                // Reverse old entry, then apply new entry
                val reversedBalance = LeaveBalanceCalculator.reverseLeave(currentBalance, oldEntry)
                val woCount = leaveRepository.getWoCountForMonth(employee.kgid, newEntry.year, newEntry.month)
                val validation = LeaveValidationEngine.validateLeave(employee, reversedBalance, newEntry, woCount)

                if (validation.isSuccess) {
                    val updatedBalance = LeaveBalanceCalculator.applyLeave(reversedBalance, newEntry)
                    leaveRepository.updateLeaveEntry(updatedBalance, newEntry.copy(id = oldEntry.id))
                    _balance.value = updatedBalance
                    refreshData(employee)
                    _uiState.value = LeaveUiState.Success
                } else {
                    val errorMsg = validation.exceptionOrNull()?.message ?: "Validation failed"
                    _uiState.value = LeaveUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating leave entry", e)
                _uiState.value = LeaveUiState.Error("Failed to update: ${e.message}")
            }
        }
    }

    /**
     * Update EL manual balance (carry-forward). Recalculates current EL balance.
     */
    fun updateElManualBalance(employee: Employee, newManualBalance: Double) {
        viewModelScope.launch {
            try {
                val currentBalance = _balance.value ?: return@launch
                val totalElTaken = _elEntries.value
                    .filter { it.elEntryType == "taken" }
                    .sumOf { it.totalDays }
                val updatedBalance = LeaveBalanceCalculator.updateElManualBalance(
                    currentBalance, newManualBalance, totalElTaken
                )
                leaveRepository.saveLeaveBalance(updatedBalance)
                _balance.value = updatedBalance
            } catch (e: Exception) {
                Log.e(TAG, "Error updating EL manual balance", e)
                _uiState.value = LeaveUiState.Error("Failed to update EL balance: ${e.message}")
            }
        }
    }

    /**
     * Update CL annual limit (10 or 15 days).
     */
    fun updateClLimit(employee: Employee, newLimit: Int) {
        viewModelScope.launch {
            try {
                val currentBalance = _balance.value ?: return@launch
                val totalClTaken = _clEntries.value.sumOf { it.totalDays }
                val updatedBalance = LeaveBalanceCalculator.updateClLimit(currentBalance, newLimit, totalClTaken)
                leaveRepository.saveLeaveBalance(updatedBalance)
                _balance.value = updatedBalance
            } catch (e: Exception) {
                Log.e(TAG, "Error updating CL limit", e)
                _uiState.value = LeaveUiState.Error("Failed to update CL limit: ${e.message}")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = LeaveUiState.Idle
    }
}

sealed class LeaveUiState {
    object Idle : LeaveUiState()
    object Loading : LeaveUiState()
    object Success : LeaveUiState()
    data class Error(val message: String) : LeaveUiState()
}
