package com.pacenote.vla.ui.dashboard

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacenote.vla.core.aibrain.VlaBrain
import com.pacenote.vla.core.domain.model.GForceVector
import com.pacenote.vla.core.domain.model.TelemetryData
import com.pacenote.vla.feature.camera.manager.CameraManager
import com.pacenote.vla.feature.sensor.manager.SensorFusionManager
import com.pacenote.vla.feature.vision.VisionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val cameraManager: CameraManager,
    private val sensorFusionManager: SensorFusionManager,
    private val visionManager: VisionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _gForce = MutableStateFlow(GForceVector.ZERO)
    val gForce: StateFlow<GForceVector> = _gForce.asStateFlow()

    private val _aiAction = MutableStateFlow<VlaBrain.VlaAction?>(null)
    val aiAction: StateFlow<VlaBrain.VlaAction?> = _aiAction.asStateFlow()

    private val _aiStatus = MutableStateFlow<VlaBrain.AiStatus>(VlaBrain.AiStatus.Idle)
    val aiStatus: StateFlow<VlaBrain.AiStatus> = _aiStatus.asStateFlow()

    private var lastFrameTime = 0L

    fun initialize() {
        viewModelScope.launch {
            try {
                // Initialize VlaBrain
                val result = visionManager.getAiStatus()
                _aiStatus.value = result

                if (result is VlaBrain.AiStatus.Idle) {
                    visionManager.initialize()
                    _aiStatus.value = visionManager.getAiStatus()
                }

                // Start sensor telemetry collection
                launch { collectTelemetry() }
                // Start AI action collection
                launch { collectAiActions() }

                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    isLoading = false
                )

                Timber.tag("DashboardViewModel").d("Initialization complete")
            } catch (e: Exception) {
                Timber.tag("DashboardViewModel").e(e, "Initialization failed")
                _aiStatus.value = VlaBrain.AiStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        viewModelScope.launch {
            try {
                val result = cameraManager.initializeCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView
                )

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isRecording = true)
                    Timber.tag("DashboardViewModel").d("Camera initialized")
                    startVisionProcessing()
                } else {
                    Timber.tag("DashboardViewModel").e(
                        result.exceptionOrNull(),
                        "Camera initialization failed"
                    )
                }
            } catch (e: Exception) {
                Timber.tag("DashboardViewModel").e(e, "Camera start failed")
            }
        }
    }

    private fun startVisionProcessing() {
        viewModelScope.launch {
            try {
                cameraManager.frameFlow.collect { imageProxy ->
                    val currentTime = System.currentTimeMillis()
                    // Rate limit: max 5 FPS for AI processing
                    if (currentTime - lastFrameTime > 200) {
                        lastFrameTime = currentTime

                        val telemetryContext = with(_gForce.value) {
                            VlaBrain.TelemetryContext(
                                gForceLongitudinal = longitudinal,
                                gForceLateral = lateral
                            )
                        }

                        visionManager.processFrame(imageProxy, telemetryContext)
                    } else {
                        imageProxy.close()
                    }
                }
            } catch (e: Exception) {
                Timber.tag("DashboardViewModel").e(e, "Vision processing failed")
            }
        }
    }

    private suspend fun collectTelemetry() {
        try {
            sensorFusionManager.telemetryFlow.collect { telemetry ->
                val gForceVector = GForceVector(
                    longitudinal = telemetry.longitudinalG,
                    lateral = telemetry.lateralG
                )
                _gForce.value = gForceVector

                // Detect maneuvers for AI sampling
                val maneuver = sensorFusionManager.detectManeuver(telemetry)
                if (maneuver != com.pacenote.vla.core.domain.model.ManeuverEvent.None) {
                    Timber.tag("DashboardViewModel").d("Maneuver detected: $maneuver")
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").e(e, "Telemetry collection failed")
        }
    }

    private suspend fun collectAiActions() {
        try {
            visionManager.aiActionFlow.collect { action ->
                if (action != null) {
                    _aiAction.value = action
                    Timber.tag("DashboardViewModel").d(
                        "AI Action: ${action.alert} - ${action.message}"
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag("DashboardViewModel").e(e, "AI action collection failed")
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
        visionManager.release()
        Timber.tag("DashboardViewModel").d("Resources released")
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isConnected: Boolean = false,
    val isRecording: Boolean = false,
    val errorMessage: String? = null
)
