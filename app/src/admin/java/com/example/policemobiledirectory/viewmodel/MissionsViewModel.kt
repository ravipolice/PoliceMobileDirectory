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
    
    // Dashboard Stats
    val totalCount: Int = 0,
    val residentCount: Int = 0,
    
    // Filters (Per Sketch)
    val countries: List<String> = emptyList(),
    val selectedCountry: String = "All Countries",
    val availableMissions: List<String> = emptyList(),
    val selectedMission: String = "All Cities",
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
                    val countries = listOf("All Countries") + missions.map { it.country }.distinct().sorted()
                    val residentCount = missions.count { it.status == "Resident" }
                    
                    _uiState.value = _uiState.value.copy(
                        missions = missions,
                        filteredMissions = missions, // Default show all
                        countries = countries,
                        totalCount = missions.size,
                        residentCount = residentCount,
                        isLoading = false
                    )
                    updateAvailableMissions()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "An unknown error occurred"
                    )
                }
            }
        }
    }

    fun onCountrySelected(country: String) {
        _uiState.value = _uiState.value.copy(
            selectedCountry = country,
            selectedMission = "All Cities" // Reset city when country changes
        )
        updateAvailableMissions()
    }

    fun onMissionSelected(mission: String) {
        _uiState.value = _uiState.value.copy(selectedMission = mission)
    }

    private fun updateAvailableMissions() {
        val missions = _uiState.value.missions
        val country = _uiState.value.selectedCountry
        
        val filteredCities = if (country == "All Countries") {
            missions.map { it.city }.distinct()
        } else {
            missions.filter { it.country == country }.map { it.city }.distinct()
        }.sorted()

        _uiState.value = _uiState.value.copy(
            availableMissions = listOf("All Cities") + filteredCities
        )
    }

    fun onSearchClicked() {
        val state = _uiState.value
        val country = state.selectedCountry
        val city = state.selectedMission
        
        val filtered = state.missions.filter { mission ->
            (country == "All Countries" || mission.country == country) &&
            (city == "All Cities" || mission.city == city)
        }
        
        _uiState.value = _uiState.value.copy(filteredMissions = filtered)
    }

    // Keep the textual search for flexibility (can be used in AppBar)
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyTextFilter()
    }

    private fun applyTextFilter() {
        val query = _uiState.value.searchQuery.lowercase()
        val filtered = if (query.isEmpty()) {
            _uiState.value.missions
        } else {
            _uiState.value.missions.filter {
                it.country.lowercase().contains(query) ||
                it.city.lowercase().contains(query) ||
                it.name.lowercase().contains(query)
            }
        }
        _uiState.value = _uiState.value.copy(filteredMissions = filtered)
    }
}
