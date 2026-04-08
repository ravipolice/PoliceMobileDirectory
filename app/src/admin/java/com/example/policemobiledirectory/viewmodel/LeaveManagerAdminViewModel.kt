package com.example.policemobiledirectory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.utils.OperationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LeaveManagerAdminViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val employeeRepo: com.example.policemobiledirectory.repository.EmployeeRepository
) : ViewModel() {

    private var refreshJob: kotlinx.coroutines.Job? = null


    private val _pendingUsers = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val pendingUsers: StateFlow<List<Map<String, Any>>> = _pendingUsers.asStateFlow()

    private val _departments = MutableStateFlow<List<String>>(emptyList())
    val departments: StateFlow<List<String>> = _departments.asStateFlow()

    private val _status = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val status: StateFlow<OperationStatus<String>> = _status.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        refreshPendingUsers()
        refreshDepartments()
    }

    fun refreshPendingUsers() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                _status.value = OperationStatus.Loading
                val snapshot = firestore.collection("users")
                    .whereEqualTo("status", "pending")
                    .get()
                    .await()
                
                _pendingUsers.value = snapshot.documents.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    data
                }.sortedByDescending { (it["createdAt"] as? Number)?.toLong() ?: 0L }
                _status.value = OperationStatus.Idle
            } catch (e: Exception) {
                Log.e("LMAdminVM", "Error fetching pending users", e)
                _status.value = OperationStatus.Error(e.message ?: "Failed to fetch users")
            }
        }
    }

    fun refreshDepartments() = viewModelScope.launch {
        try {
            val snapshot = firestore.collection("leave_manager_departments")
                .orderBy("name")
                .get()
                .await()
            _departments.value = snapshot.documents.mapNotNull { it.getString("name") }
        } catch (e: Exception) {
            Log.e("LMAdminVM", "Error fetching departments", e)
        }
    }

    fun approveUser(kgid: String) = viewModelScope.launch {
        try {
            _status.value = OperationStatus.Loading
            firestore.collection("users").document(kgid).update("status", "approved").await()
            _pendingUsers.value = _pendingUsers.value.filter { it["id"] != kgid }
            _status.value = OperationStatus.Success("User approved successfully")
        } catch (e: Exception) {
            _status.value = OperationStatus.Error("Approval failed: ${e.message}")
        }
    }

    fun rejectUser(kgid: String) = viewModelScope.launch {
        try {
            _status.value = OperationStatus.Loading
            firestore.collection("users").document(kgid).update("status", "rejected").await()
            _pendingUsers.value = _pendingUsers.value.filter { it["id"] != kgid }
            _status.value = OperationStatus.Success("User rejected")
        } catch (e: Exception) {
            _status.value = OperationStatus.Error("Rejection failed: ${e.message}")
        }
    }

    fun addDepartment(name: String) = viewModelScope.launch {
        try {
            _status.value = OperationStatus.Loading
            firestore.collection("leave_manager_departments").add(mapOf("name" to name)).await()
            refreshDepartments()
            _status.value = OperationStatus.Success("Department added")
        } catch (e: Exception) {
            _status.value = OperationStatus.Error("Failed to add department")
        }
    }
}
