package com.example.policemobiledirectory.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.model.Employee
import com.example.policemobiledirectory.repository.EmployeeRepository
import com.example.policemobiledirectory.repository.ImageRepository
import com.example.policemobiledirectory.utils.OperationStatus
import com.example.policemobiledirectory.repository.RepoResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditEmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository,
    val imageRepository: ImageRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _employee = MutableStateFlow<Employee?>(null)
    val employee = _employee.asStateFlow()

    private val _saveStatus = MutableStateFlow<RepoResult<Boolean>?>(null)
    val saveStatus = _saveStatus.asStateFlow()

    private val _photoUploadStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val photoUploadStatus = _photoUploadStatus.asStateFlow()

    init {
        savedStateHandle.get<String>("employeeId")?.let { employeeId ->
            if (employeeId.isNotEmpty()) {
                viewModelScope.launch {
                    employeeRepository.getEmployeeByKgid(employeeId).collect { repoResult ->
                        if (repoResult is RepoResult.Success) {
                            _employee.value = repoResult.data
                        }
                    }
                }
            }
        }
    }

    /**
     * Used by:
     *  - Admin Add Employee
     *  - Admin Edit Employee
     *  - MyProfileEdit (self-edit)  <-- new requirement
     */
    fun saveEmployee(employee: Employee, newPhotoUri: Uri?) {
        viewModelScope.launch {
            try {
                _saveStatus.value = RepoResult.Loading
                
                // 1️⃣ Duplicate check (only for new employees or changed identifiers)
                val originalKgid = savedStateHandle.get<String>("employeeId")
                val isNewUser = originalKgid.isNullOrEmpty()
                
                val duplicateError = employeeRepository.checkDuplicates(
                    kgid = employee.kgid,
                    email = employee.email ?: "",
                    excludeKgid = if (!isNewUser) originalKgid else null
                )
                
                if (duplicateError != null) {
                    _saveStatus.value = RepoResult.Error(message = duplicateError)
                    return@launch
                }

                var updatedEmployee = employee

                // Upload new photo if provided
                if (newPhotoUri != null) {
                    var uploadFailed = false
                    try {
                        imageRepository.uploadOfficerImage(newPhotoUri, employee.kgid).collect { status ->
                            _photoUploadStatus.value = status

                            when (status) {
                                is OperationStatus.Success -> {
                                    status.data?.let { url ->
                                        updatedEmployee = updatedEmployee.copy(photoUrl = url)
                                    }
                                }
                                is OperationStatus.Error -> {
                                    _saveStatus.value = RepoResult.Error(message = status.message)
                                    uploadFailed = true
                                }
                                else -> Unit
                            }
                        }
                    } catch (e: Exception) {
                        _saveStatus.value = RepoResult.Error(message = "Upload failed: ${e.message}")
                        uploadFailed = true
                    }

                    _photoUploadStatus.value = OperationStatus.Idle
                    if (uploadFailed) return@launch
                }

                // Save to DB + Google Sheet
                _saveStatus.value = RepoResult.Loading
                employeeRepository.addOrUpdateEmployee(updatedEmployee).collect { result ->
                    _saveStatus.value = result
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _saveStatus.value = RepoResult.Error(message = "Unexpected error: ${e.message}")
            }
        }
    }

    // 👇 New helper for MyProfileEditScreen — calls same function
    fun updateMyProfile(employee: Employee, newPhotoUri: Uri?) {
        saveEmployee(employee, newPhotoUri)
    }

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

    fun resetPhotoStatus() {
        _photoUploadStatus.value = OperationStatus.Idle
    }

    /**
     * Upload photo and return the URL via callback
     */
    suspend fun uploadPhotoAndGetUrl(photoUri: Uri, kgid: String, onComplete: (String?) -> Unit) {
        imageRepository.uploadOfficerImage(photoUri, kgid).collect { status ->
            _photoUploadStatus.value = status
            when (status) {
                is OperationStatus.Success -> {
                    onComplete(status.data)
                    _photoUploadStatus.value = OperationStatus.Idle
                }
                is OperationStatus.Error -> {
                    onComplete(null)
                    _photoUploadStatus.value = OperationStatus.Idle
                }
                else -> Unit
            }
        }
    }
}
