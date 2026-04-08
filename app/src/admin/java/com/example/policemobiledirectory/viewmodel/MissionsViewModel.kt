package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.remote.Mission
import com.example.policemobiledirectory.repository.MissionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MissionsUiState(
    val missions: List<Mission> = emptyList(),
    val filteredMissions: List<Mission> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class MissionsViewModel @Inject constructor(
    private val repository: MissionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionsUiState())
    val uiState: StateFlow<MissionsUiState> = _uiState.asStateFlow()

    init {
        fetchMissions()
    }

    fun fetchMissions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.getMissions(forceRefresh).collect { result ->
                result.onSuccess { missions ->
                    _uiState.value = _uiState.value.copy(
                        missions = missions,
                        isLoading = false
                    )
                    applyFilter()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "An unknown error occurred"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    private fun applyFilter() {
        val query = _uiState.value.searchQuery.lowercase()
        val filtered = if (query.isEmpty()) {
            _uiState.value.missions
        } else {
            _uiState.value.missions.filter {
                it.country.lowercase().contains(query) ||
                it.city.lowercase().contains(query) ||
                it.name.lowercase().contains(query) ||
                it.region.lowercase().contains(query)
            }
        }
        _uiState.value = _uiState.value.copy(filteredMissions = filtered)
    }
}
