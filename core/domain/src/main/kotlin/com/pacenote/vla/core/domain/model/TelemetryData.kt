package com.pacenote.vla.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Core telemetry data structure for sensor fusion
 */
@Serializable
data class TelemetryData(
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f, // meters per second
    val bearing: Float = 0f,
    val accuracy: Float = 0f,

    // IMU Data
    val accelerationX: Float = 0f, // m/s²
    val accelerationY: Float = 0f,
    val accelerationZ: Float = 0f,
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    val gyroscopeX: Float = 0f, // rad/s
    val gyroscopeY: Float = 0f,
    val gyroscopeZ: Float = 0f,

    // Computed values
    val longitudinalG: Float = 0f, // Acceleration/Braking G
    val lateralG: Float = 0f,      // Cornering G
    val yawRate: Float = 0f,       // Yaw rate in deg/s
) {
    companion object {
        val EMPTY = TelemetryData()
    }
}

/**
 * Computed G-Force vector for HUD visualization
 */
@Serializable
data class GForceVector(
    val longitudinal: Float, // Positive = acceleration, Negative = braking
    val lateral: Float,      // Positive = right turn, Negative = left turn
    val magnitude: Float = kotlin.math.sqrt(longitudinal * longitudinal + lateral * lateral).toFloat()
) {
    companion object {
        val ZERO = GForceVector(0f, 0f)
    }
}

/**
 * Critical maneuver detection result
 */
@Serializable
sealed class ManeuverEvent {
    @Serializable
    data object None : ManeuverEvent()

    @Serializable
    data class HardBraking(
        val maxDeceleration: Float, // Negative G value
        val duration: Long // ms
    ) : ManeuverEvent()

    @Serializable
    data class HardAcceleration(
        val maxAcceleration: Float,
        val duration: Long
    ) : ManeuverEvent()

    @Serializable
    data class SharpTurn(
        val maxLateralG: Float,
        val direction: TurnDirection,
        val duration: Long
    ) : ManeuverEvent()

    enum class TurnDirection { LEFT, RIGHT }
}

/**
 * Sampling mode for VLA (Vision-Language-Action)
 */
enum class VlaSamplingMode {
    CRUISING,      // Low frequency (15s) - normal driving
    MANEUVER,      // High frequency (2-4s) - during high G events
    CRITICAL,      // Immediate (1s) - during critical maneuvers
    IDLE           // Suspended - not driving
}

/**
 * ROI (Region of Interest) configuration
 */
@Serializable
data class RoiConfig(
    val isLhd: Boolean = true, // Left Hand Drive vs Right Hand Drive
    val mirrorRoi: RectF = RectF(0.7f, 0.0f, 1.0f, 0.4f),
    val windshieldRoi: RectF = RectF(0.0f, 0.0f, 1.0f, 0.6f)
) {
    @Serializable
    data class RectF(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
}
