package com.example.policemobiledirectory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.policemobiledirectory.data.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val sessionManager: SessionManager
) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.isDarkTheme.collect {
                _isDarkTheme.value = it
            }
        }
        viewModelScope.launch {
            sessionManager.fontScale.collect {
                _fontScale.value = it
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            sessionManager.setDarkTheme(!_isDarkTheme.value)
        }
    }

    fun adjustFontScale(increase: Boolean) {
        val step = 0.1f
        val current = _fontScale.value
        val newScale = if (increase) {
            (current + step).coerceAtMost(1.8f)
        } else {
            (current - step).coerceAtLeast(0.8f)
        }
        viewModelScope.launch {
            sessionManager.setFontScale(newScale)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            sessionManager.setFontScale(scale.coerceIn(0.8f, 1.8f))
        }
    }
}
