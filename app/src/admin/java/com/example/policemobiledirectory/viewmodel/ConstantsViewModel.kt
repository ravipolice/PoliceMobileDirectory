package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.repository.ConstantsRepository
import com.example.policemobiledirectory.model.UnitModel
import com.example.policemobiledirectory.utils.OperationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConstantsViewModel @Inject constructor(
    private val adminRepo: ConstantsRepository
) : BaseConstantsViewModel(adminRepo) {

    private val _adminResults = MutableStateFlow<Result<String>?>(null)
    val adminResults: StateFlow<Result<String>?> = _adminResults.asStateFlow()

    private val _refreshStatus = MutableStateFlow<OperationStatus<String>>(OperationStatus.Idle)
    val refreshStatus: StateFlow<OperationStatus<String>> = _refreshStatus.asStateFlow()

    fun resetRefreshStatus() {
        _refreshStatus.value = OperationStatus.Idle
    }

    private val _currentUnitSections = MutableStateFlow<List<String>>(emptyList())
    val currentUnitSections: StateFlow<List<String>> = _currentUnitSections.asStateFlow()

    fun loadSectionsForUnit(unitName: String) {
        viewModelScope.launch {
            _currentUnitSections.value = adminRepo.getSectionsForUnit(unitName)
        }
    }

    fun addDistrict(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.addDistrict(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("District added") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun deleteDistrict(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.deleteDistrict(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("District deleted") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateDistrict(oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateDistrict(oldName, newName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("District updated") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun addStation(district: String, name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.addStation(district, name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Station added") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun deleteStation(district: String, name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.deleteStation(district, name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Station deleted") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateStation(district: String, oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateStation(district, oldName, newName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Station updated") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun addUnit(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.addUnit(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Unit added") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun deleteUnit(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.deleteUnit(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Unit deleted") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateUnit(oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateUnit(oldName, newName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Unit updated") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateUnitDetails(unit: UnitModel) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateUnitDetails(unit)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Unit details updated") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun addRank(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.addRank(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Rank added") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun deleteRank(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.deleteRank(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Rank deleted") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateRank(oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateRank(oldName, newName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Rank updated") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun addSection(unitName: String, sectionName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.addSection(unitName, sectionName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Section added") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            _currentUnitSections.value = adminRepo.getSectionsForUnit(unitName)
            updateLocalState()
        }
    }

    fun deleteSection(unitName: String, sectionName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.deleteSection(unitName, sectionName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Section deleted") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            _currentUnitSections.value = adminRepo.getSectionsForUnit(unitName)
            updateLocalState()
        }
    }

    fun addSubSection(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.addSubSection(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Sub-section added") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun deleteSubSection(name: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.deleteSubSection(name)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Sub-section deleted") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateSubSection(oldName: String, newName: String) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateSubSection(oldName, newName)
            _refreshStatus.value = if (result.isSuccess) OperationStatus.Success("Sub-section updated") else OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun updateDutyRoleMapping(unit: String, roles: List<String>) {
        viewModelScope.launch {
            _refreshStatus.value = OperationStatus.Loading
            val result = adminRepo.updateDutyRoleMapping(unit, roles)
            _refreshStatus.value = if (result.isSuccess) 
                OperationStatus.Success("Mapping updated for $unit") 
            else 
                OperationStatus.Error(result.exceptionOrNull()?.message ?: "Error")
            updateLocalState()
        }
    }

    fun clearAdminResult() {
        _adminResults.value = null
    }
}
