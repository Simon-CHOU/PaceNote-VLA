package com.pacenote.vla.feature.livekit.manager

import com.pacenote.vla.core.domain.model.DetectionResult
import com.pacenote.vla.core.domain.model.ManeuverEvent
import com.pacenote.vla.core.domain.model.ReflexAlert
import com.pacenote.vla.core.domain.model.TelemetryData
import com.pacenote.vla.core.domain.model.VlaSamplingMode
import com.pacenote.vla.feature.livekit.client.ConnectionState
import com.pacenote.vla.feature.livekit.client.LiveKitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages VLA (Vision-Language-Action) session with LiveKit
 *
 * Implements adaptive sampling strategy:
 * - IDLE: Not driving, no sampling
 * - CRUISING: Normal driving, 15s interval
 * - MANEUVER: High G events, 2-4s interval
 * - CRITICAL: Extreme maneuvers, 1s interval
 */
@Singleton
class VlaSessionManager @Inject constructor(
    private val liveKitClient: LiveKitClient
) {
    private val scope = CoroutineScope(SupervisorJob())

    private var samplingJob: Job? = null
    private var telemetryJob: Job? = null
    private var currentSamplingMode: VlaSamplingMode = VlaSamplingMode.IDLE

    private val _isActive = MutableStateFlow(false)
    val isActiveFlow: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionStateFlow: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Thresholds for mode transitions
    private val HIGH_G_THRESHOLD = 0.3f // Trigger MANEUVER mode
    private val CRITICAL_G_THRESHOLD = 0.5f // Trigger CRITICAL mode
    private val MOVING_THRESHOLD = 2.0f // m/s (~7 km/h) to consider "driving"

    /**
     * Start VLA session with auto-connect
     */
    suspend fun startSession(): Result<Unit> {
        Timber.i("=== Starting VLA Session ===")

        // Connect to LiveKit
        val connectResult = liveKitClient.connect()
        connectResult.onFailure { e ->
            Timber.e(e, "Failed to connect to LiveKit")
            return connectResult
        }

        // Publish video and audio
        liveKitClient.publishVideo()
        liveKitClient.publishAudio()

        _isActive.value = true
        _connectionState.value = ConnectionState.Connecting

        // Listen for connection state changes
        scope.launch {
            liveKitClient.connectionStateFlow.collect { state ->
                _connectionState.value = state
                when (state) {
                    is ConnectionState.Connected -> {
                        Timber.i("✓ VLA Session connected - starting data pipeline")
                        startTelemetryStream()
                        startSamplingLoop()
                    }
                    is ConnectionState.Error -> {
                        Timber.e("✗ VLA Session error: ${state.message}")
                        samplingJob?.cancel()
                    }
                    else -> {}
                }
            }
        }

        return Result.success(Unit)
    }

    /**
     * Stop VLA session
     */
    suspend fun stopSession() {
        Timber.i("=== Stopping VLA Session ===")

        samplingJob?.cancel()
        telemetryJob?.cancel()
        samplingJob = null
        telemetryJob = null

        liveKitClient.disconnect()
        _isActive.value = false
        _connectionState.value = ConnectionState.Disconnected
        currentSamplingMode = VlaSamplingMode.IDLE

        Timber.i("VLA Session stopped")
    }

    /**
     * Start continuous telemetry stream via DataChannel
     */
    private fun startTelemetryStream() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            // Send telemetry every 200ms (5Hz)
            while (_isActive.value && _connectionState.value is ConnectionState.Connected) {
                // Note: Actual telemetry data will be sent by the caller via processTelemetry
                delay(200)
            }
        }
    }

    /**
     * Process telemetry and update sampling mode
     */
    fun processTelemetry(telemetry: TelemetryData) {
        if (!_isActive.value) return

        // Send telemetry via DataChannel
        scope.launch {
            try {
                liveKitClient.sendTelemetryData(telemetry)
            } catch (e: Exception) {
                Timber.w(e, "Failed to send telemetry")
            }
        }

        val gForce = kotlin.math.sqrt(
            telemetry.longitudinalG * telemetry.longitudinalG +
            telemetry.lateralG * telemetry.lateralG
        ).toFloat()

        val isMoving = telemetry.speedMps > MOVING_THRESHOLD

        val newMode = when {
            !isMoving -> VlaSamplingMode.IDLE
            abs(telemetry.longitudinalG) > CRITICAL_G_THRESHOLD ||
            abs(telemetry.lateralG) > CRITICAL_G_THRESHOLD -> VlaSamplingMode.CRITICAL
            gForce > HIGH_G_THRESHOLD -> VlaSamplingMode.MANEUVER
            else -> VlaSamplingMode.CRUISING
        }

        if (newMode != currentSamplingMode) {
            Timber.d("VLA mode: $currentSamplingMode → $newMode (G: $gForce)")
            currentSamplingMode = newMode
            restartSamplingLoop()
        }
    }

    /**
     * Process detection result from MediaPipe
     */
    fun processDetectionResult(detection: DetectionResult) {
        if (!_isActive.value) return

        scope.launch {
            detection.objects.forEachIndexed { index, obj ->
                liveKitClient.sendDetectionResult(
                    objectId = index,
                    className = obj.className,
                    confidence = obj.confidence
                )

                // Trigger barge-in for blind spot detection
                if (obj.isInBlindSpot) {
                    liveKitClient.bargeIn()
                    // Resume after 3 seconds
                    delay(3000)
                    liveKitClient.resumeRemoteAudio()
                }
            }
        }
    }

    /**
     * Handle reflex alert (local detection)
     */
    fun handleReflexAlert(alert: ReflexAlert) {
        when (alert) {
            is ReflexAlert.BlindSpotObject -> {
                Timber.w("⚠️ BLIND SPOT: ${alert.objectClass} (${String.format("%.2f", alert.confidence)})")
                liveKitClient.bargeIn()
                currentSamplingMode = VlaSamplingMode.CRITICAL

                scope.launch {
                    liveKitClient.sendManeuverAlert(
                        type = "blind_spot_detection",
                        severity = "high",
                        description = "Detected ${alert.objectClass} in blind spot at ${alert.position}"
                    )
                }
            }
            is ReflexAlert.CriticalManeuver -> {
                Timber.w("⚠️ CRITICAL: ${alert.maneuverType} (${alert.gForce}G)")
                liveKitClient.bargeIn()
                currentSamplingMode = VlaSamplingMode.CRITICAL

                scope.launch {
                    liveKitClient.sendManeuverAlert(
                        type = alert.maneuverType,
                        severity = "critical",
                        description = "Critical maneuver: ${alert.maneuverType} at ${alert.gForce}G"
                    )
                }
            }
            else -> {}
        }
        restartSamplingLoop()
    }

    /**
     * Handle maneuver event
     */
    fun handleManeuverEvent(event: ManeuverEvent) {
        when (event) {
            is ManeuverEvent.HardBraking -> {
                Timber.w("⚠️ HARD BRAKING: ${event.maxDeceleration}G")
                scope.launch {
                    liveKitClient.sendManeuverAlert(
                        type = "hard_braking",
                        severity = "high",
                        description = "Hard braking at ${event.maxDeceleration}G"
                    )
                }
                currentSamplingMode = VlaSamplingMode.MANEUVER
            }
            is ManeuverEvent.HardAcceleration -> {
                Timber.w("⚠️ HARD ACCELERATION: ${event.maxAcceleration}G")
                scope.launch {
                    liveKitClient.sendManeuverAlert(
                        type = "hard_acceleration",
                        severity = "medium",
                        description = "Hard acceleration at ${event.maxAcceleration}G"
                    )
                }
                currentSamplingMode = VlaSamplingMode.MANEUVER
            }
            is ManeuverEvent.SharpTurn -> {
                Timber.w("⚠️ SHARP TURN: ${event.maxLateralG}G (${event.direction})")
                scope.launch {
                    liveKitClient.sendManeuverAlert(
                        type = "sharp_turn",
                        severity = "high",
                        description = "Sharp ${event.direction} turn at ${event.maxLateralG}G"
                    )
                }
                currentSamplingMode = VlaSamplingMode.MANEUVER
            }
            else -> {}
        }
        restartSamplingLoop()
    }

    private fun startSamplingLoop() {
        restartSamplingLoop()
    }

    private fun restartSamplingLoop() {
        samplingJob?.cancel()
        samplingJob = scope.launch {
            while (_isActive.value) {
                val interval = when (currentSamplingMode) {
                    VlaSamplingMode.IDLE -> 0L // Don't sample
                    VlaSamplingMode.CRUISING -> 15000L
                    VlaSamplingMode.MANEUVER -> 3000L
                    VlaSamplingMode.CRITICAL -> 1000L
                }

                if (interval > 0) {
                    // Trigger frame capture and send to VLM
                    // This is handled by the camera frame processor
                    Timber.v("Sampling frame (mode: $currentSamplingMode)")
                    delay(interval)
                } else {
                    delay(1000L) // Check again in 1s
                }
            }
        }
    }

    /**
     * Resume remote audio after barge-in period
     */
    suspend fun resumeAudio() {
        liveKitClient.resumeRemoteAudio()
    }

    /**
     * Get current connection state
     */
    val isConnected: Boolean
        get() = _connectionState.value is ConnectionState.Connected

    val currentState: ConnectionState
        get() = _connectionState.value

    /**
     * Get error message if any
     */
    fun getErrorMessage(): String? {
        return when (val state = _connectionState.value) {
            is ConnectionState.Error -> state.message
            else -> liveKitClient.errorMessageFlow.value
        }
    }
}
