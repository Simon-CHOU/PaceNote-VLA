package com.pacenote.vla.feature.sensor.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.pacenote.vla.core.domain.model.GForceVector
import com.pacenote.vla.core.domain.model.ManeuverEvent
import com.pacenote.vla.core.domain.model.TelemetryData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Manages IMU sensor fusion for G-Force and maneuver detection
 * Target: 50Hz sampling rate as per spec
 */
@Singleton
class SensorFusionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Thresholds for maneuver detection
    private val HARD_BRAKING_THRESHOLD = -0.4f // G
    private val HARD_ACCELERATION_THRESHOLD = 0.35f // G
    private val SHARP_TURN_THRESHOLD = 0.3f // Lateral G

    /**
     * Telemetry data flow combining all sensors
     */
    val telemetryFlow: Flow<TelemetryData> = combine(
        accelerometerFlow(),
        gravityFlow(),
        gyroscopeFlow()
    ) { accel, gravity, gyro ->
        fuseSensorData(accel, gravity, gyro)
    }

    private fun accelerometerFlow() = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(Triple(it.values[0], it.values[1], it.values[2]))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST // ~200Hz (will be throttled to 50Hz)
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun gravityFlow() = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(Triple(it.values[0], it.values[1], it.values[2]))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        sensorManager.registerListener(
            listener,
            gravity,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun gyroscopeFlow() = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(Triple(it.values[0], it.values[1], it.values[2]))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(
            listener,
            gyroscope,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun fuseSensorData(
        accel: Triple<Float, Float, Float>,
        gravity: Triple<Float, Float, Float>,
        gyro: Triple<Float, Float, Float>
    ): TelemetryData {
        // Remove gravity from accelerometer to get linear acceleration
        val linearAccelX = accel.first - gravity.first
        val linearAccelY = accel.second - gravity.second
        val linearAccelZ = accel.third - gravity.third

        // Calculate G-Force in vehicle coordinate system
        // Assuming portrait orientation, phone mounted in landscape
        val longitudinalG = linearAccelY / 9.81f  // Forward/backward
        val lateralG = linearAccelX / 9.81f      // Left/right

        // Convert gyroscope Z from rad/s to deg/s for yaw rate
        val yawRate = Math.toDegrees(gyro.third.toDouble()).toFloat()

        return TelemetryData(
            timestamp = System.currentTimeMillis(),
            accelerationX = accel.first,
            accelerationY = accel.second,
            accelerationZ = accel.third,
            gravityX = gravity.first,
            gravityY = gravity.second,
            gravityZ = gravity.third,
            gyroscopeX = gyro.first,
            gyroscopeY = gyro.second,
            gyroscopeZ = gyro.third,
            longitudinalG = longitudinalG,
            lateralG = lateralG,
            yawRate = yawRate
        )
    }

    /**
     * Detect maneuvers from G-Force data
     */
    fun detectManeuver(telemetry: TelemetryData): ManeuverEvent {
        val gForce = GForceVector(
            longitudinal = telemetry.longitudinalG,
            lateral = telemetry.lateralG
        )

        return when {
            telemetry.longitudinalG < HARD_BRAKING_THRESHOLD -> {
                ManeuverEvent.HardBraking(
                    maxDeceleration = telemetry.longitudinalG,
                    duration = 0L // Will be calculated over time
                )
            }
            telemetry.longitudinalG > HARD_ACCELERATION_THRESHOLD -> {
                ManeuverEvent.HardAcceleration(
                    maxAcceleration = telemetry.longitudinalG,
                    duration = 0L
                )
            }
            abs(telemetry.lateralG) > SHARP_TURN_THRESHOLD -> {
                ManeuverEvent.SharpTurn(
                    maxLateralG = abs(telemetry.lateralG),
                    direction = if (telemetry.lateralG > 0) {
                        ManeuverEvent.TurnDirection.RIGHT
                    } else {
                        ManeuverEvent.TurnDirection.LEFT
                    },
                    duration = 0L
                )
            }
            else -> ManeuverEvent.None
        }
    }

    /**
     * Get current hardware tier for NPU/GPU/CPU fallback
     */
    fun getHardwareTier(): HardwareTier {
        val hasGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val hasAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        return if (hasGyro && hasAccel) {
            HardwareTier.TIER_1
        } else if (hasAccel) {
            HardwareTier.TIER_2
        } else {
            HardwareTier.TIER_3
        }
    }

    enum class HardwareTier {
        TIER_1,  // Full sensor suite (SD 8 Gen 3)
        TIER_2,  // Partial sensors (Legacy)
        TIER_3   // Minimal sensors (Fallback)
    }
}
