package com.pacenote.vla.feature.telemetry.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacenote.vla.core.domain.model.RoiConfig
import com.pacenote.vla.core.domain.model.TelemetryData
import com.pacenote.vla.feature.livekit.client.ConnectionState
import com.pacenote.vla.feature.livekit.manager.VlaSessionManager
import com.pacenote.vla.feature.sensor.manager.LocationManager
import com.pacenote.vla.feature.sensor.manager.SensorFusionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for telemetry and drive assistant coordination
 * Integrates sensors and LiveKit VLA session
 *
 * NOTE: Camera functionality removed for mock environment
 */
@HiltViewModel
class TelemetryViewModel @Inject constructor(
    private val sensorFusionManager: SensorFusionManager,
    private val locationManager: LocationManager,
    private val vlaSessionManager: VlaSessionManager
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunningFlow: StateFlow<Boolean> = _isRunning.asStateFlow()

    // LiveKit connection states
    val connectionStateFlow: StateFlow<ConnectionState> = vlaSessionManager.connectionStateFlow

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow: StateFlow<String?> = _errorMessage.asStateFlow()

    val isVlaActiveFlow: StateFlow<Boolean> = vlaSessionManager.isActiveFlow

    // Hardcoded ROI config (LHD by default)
    private val _roiConfig = MutableStateFlow(RoiConfig(isLhd = true))
    val roiConfigFlow: StateFlow<RoiConfig> = _roiConfig.asStateFlow()

    // Combined telemetry from IMU and GPS
    val telemetryFlow: Flow<TelemetryData> = combine(
        sensorFusionManager.telemetryFlow,
        locationManager.locationFlow()
    ) { imu, gps ->
        // Merge IMU and GPS data
        val mergedTelemetry = imu.copy(
            latitude = gps.latitude,
            longitude = gps.longitude,
            altitude = gps.altitude,
            speedMps = gps.speedMps,
            bearing = gps.bearing,
            accuracy = gps.accuracy
        )

        // Send to LiveKit if VLA session is active
        if (vlaSessionManager.isConnected) {
            vlaSessionManager.processTelemetry(mergedTelemetry)

            // Detect and handle maneuvers
            val maneuverEvent = sensorFusionManager.detectManeuver(mergedTelemetry)
            if (maneuverEvent !is com.pacenote.vla.core.domain.model.ManeuverEvent.None) {
                vlaSessionManager.handleManeuverEvent(maneuverEvent)
            }
        }

        mergedTelemetry
    }

    /**
     * Start VLA session (auto-connects to LiveKit)
     */
    fun startVlaSession() {
        viewModelScope.launch {
            val result = vlaSessionManager.startSession()
            result.onSuccess {
                Timber.i("VLA Session started successfully")
                _isRunning.value = true
            }.onFailure { e ->
                Timber.e(e, "Failed to start VLA Session")
                _errorMessage.value = e.message
            }
        }
    }

    /**
     * Stop VLA session
     */
    fun stopVlaSession() {
        viewModelScope.launch {
            vlaSessionManager.stopSession()
            _isRunning.value = false
        }
    }

    /**
     * Start assistant (enables telemetry processing)
     */
    fun startAssistant() {
        viewModelScope.launch {
            _isRunning.value = true
            Timber.i("Drive assistant started")
        }
    }

    /**
     * Stop assistant (disables telemetry processing)
     */
    fun stopAssistant() {
        viewModelScope.launch {
            _isRunning.value = false
            stopVlaSession()
            Timber.i("Drive assistant stopped")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAssistant()
    }
}
