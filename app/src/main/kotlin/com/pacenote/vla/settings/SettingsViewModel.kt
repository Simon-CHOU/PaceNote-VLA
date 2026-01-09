package com.pacenote.vla.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacenote.vla.core.domain.model.RoiConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Settings ViewModel
 * Handles app settings including language and LHD/RHD
 *
 * NOTE: Uses mock in-memory preferences for testing
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _isLhd = MutableStateFlow(true)
    val isLhd: StateFlow<Boolean> = _isLhd.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(true)
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    /**
     * Toggle between LHD and RHD
     * Updates ROI configuration accordingly
     */
    fun toggleLhdRhd() {
        viewModelScope.launch {
            _isLhd.value = !_isLhd.value
        }
    }

    /**
     * Set language (en or zh)
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            _language.value = languageCode
            // Update app locale
            setAppLocale(languageCode)
        }
    }

    /**
     * Toggle voice output
     */
    fun toggleVoice() {
        viewModelScope.launch {
            _voiceEnabled.value = !_voiceEnabled.value
        }
    }

    /**
     * Toggle haptic feedback
     */
    fun toggleHaptic() {
        viewModelScope.launch {
            _hapticEnabled.value = !_hapticEnabled.value
        }
    }

    /**
     * Get ROI configuration based on LHD/RHD setting
     */
    fun getRoiConfig(): RoiConfig {
        return RoiConfig(isLhd = _isLhd.value)
    }

    private fun setAppLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        // Note: In a real app, you'd need to recreate the activity to apply locale changes
        // This is a simplified version
    }
}
