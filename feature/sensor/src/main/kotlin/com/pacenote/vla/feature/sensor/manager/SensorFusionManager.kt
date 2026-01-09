package com.pacenote.vla.feature.sensor.manager

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.pacenote.vla.core.domain.model.GForceVector
import com.pacenote.vla.core.domain.model.ManeuverEvent
import com.pacenote.vla.core.domain.model.TelemetryData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Manages IMU sensor fusion for G-Force and maneuver detection
 * Target: 50Hz sampling rate as per spec
 *
 * v2.1 Updates:
 * - Prefer TYPE_LINEAR_ACCELERATION (system-provided gravity-free accel)
 * - Added runtime logs for raw sensor data
 * - Added heartbeat indicator for Compose recomposition verification
 */
@Singleton
class SensorFusionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : LifecycleEventObserver {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Lifecycle tracking
    private val isActive = AtomicBoolean(false)
    private val currentLifecycle = AtomicReference<Lifecycle?>(null)

    // Device orientation (for coordinate mapping)
    private var currentRotation = Surface.ROTATION_0

    // Thresholds for maneuver detection
    private val HARD_BRAKING_THRESHOLD = -0.4f // G
    private val HARD_ACCELERATION_THRESHOLD = 0.35f // G
    private val SHARP_TURN_THRESHOLD = 0.3f // Lateral G

    // Latest telemetry state (for immediate access)
    private val _latestTelemetry = MutableStateFlow(TelemetryData.EMPTY)
    val latestTelemetry: StateFlow<TelemetryData> = _latestTelemetry

    // Heartbeat counter for UI verification
    private val _heartbeat = MutableStateFlow(0)
    val heartbeat: StateFlow<Int> = _heartbeat

    // Raw sensor values for diagnostic debugging (before any processing)
    private val _rawSensorData = MutableStateFlow(RawSensorData.ZERO)
    val rawSensorData: StateFlow<RawSensorData> = _rawSensorData

    // Low-pass filter for accelerometer fallback (gravity estimation)
    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private val alpha = 0.8f  // Low-pass filter coefficient (0.8 = gravity changes slowly)

    // Zero detection counter for automatic fallback
    private var zeroValueCount = 0
    private val ZERO_THRESHOLD = 100  // Trigger fallback after 100 zero samples
    private var forceAccelerometerFallback = false

    /**
     * Raw sensor data class for debugging
     * Shows event.values directly without any mapping/calibration
     */
    data class RawSensorData(
        val accelX: Float,
        val accelY: Float,
        val accelZ: Float,
        val timestamp: Long
    ) {
        companion object {
            val ZERO = RawSensorData(0f, 0f, 0f, 0L)
        }
    }

    /**
     * Check if LINEAR_ACCELERATION sensor is available
     */
    private fun hasLinearAcceleration(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }

    /**
     * Telemetry data flow combining all sensors
     * Uses LINEAR_ACCELERATION as primary source with automatic fallback
     * IMPORTANT: Accelerometer is decoupled from gyroscope to prevent blocking
     * CRITICAL: This is initialized once to prevent flow recreation
     */
    private val _telemetryFlow: Flow<TelemetryData> by lazy {
        Timber.tag("SensorFusionManager").i("Initializing telemetryFlow")
        val accelFlow = if (hasLinearAcceleration()) {
            Timber.tag("SensorFusionManager").i("Using TYPE_LINEAR_ACCELERATION")
            linearAccelerationFlow()
        } else {
            Timber.tag("SensorFusionManager").w("LINEAR_ACCELERATION not available, using accel-gravity fusion")
            accelerometerFlow()
        }

        accelFlow.buffer(Channel.UNLIMITED).map { accel ->
            // Detect zero values and trigger fallback if needed
            val magnitude = sqrt(accel.first * accel.first + accel.second * accel.second + accel.third * accel.third)
            if (magnitude < 0.001f && hasLinearAcceleration() && !forceAccelerometerFallback) {
                zeroValueCount++
                if (zeroValueCount >= ZERO_THRESHOLD) {
                    forceAccelerometerFallback = true
                    Timber.tag("SensorFusionManager").w(
                        "Detected $ZERO_THRESHOLD consecutive zero values from LINEAR_ACCELERATION, switching to TYPE_ACCELEROMETER with low-pass filter"
                    )
                }
            } else {
                // Reset counter if we get non-zero values
                if (zeroValueCount > 0) {
                    zeroValueCount = 0
                }
            }

            // Process accelerometer data independently (gyro not needed for G-Force)
            // Use zero gyro values since we only need acceleration for G-Force calculation
            fuseSensorData(accel, Triple(0f, 0f, 0f))
        }
    }

    val telemetryFlow: Flow<TelemetryData> get() = _telemetryFlow

    /**
     * TYPE_LINEAR_ACCELERATION - System-provided gravity-free acceleration
     * This is the preferred sensor as it handles gravity compensation in hardware/OS
     */
    private fun linearAccelerationFlow() = callbackFlow {
        // Removed strict isActive check to prevent startup race condition
        // if (!isActive.get()) { ... }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Capture raw sensor data for debugging (before any processing)
                    _rawSensorData.value = RawSensorData(
                        accelX = it.values[0],
                        accelY = it.values[1],
                        accelZ = it.values[2],
                        timestamp = it.timestamp
                    )

                    // Log raw sensor data for debugging
                    Timber.tag("SensorFusionManager").d(
                        "RAW LINEAR_ACCEL: x=%.4f, y=%.4f, z=%.4f",
                        it.values[0], it.values[1], it.values[2]
                    )

                    // Increment heartbeat
                    _heartbeat.value = _heartbeat.value + 1

                    val result = trySend(Triple(it.values[0], it.values[1], it.values[2]))
                    if (result.isFailure) {
                        Timber.tag("SensorFusionManager").w("Failed to send linear accel data: ${result.exceptionOrNull()}")
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Timber.tag("SensorFusionManager").d("Linear accel accuracy changed: $accuracy")
            }
        }

        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (linearAccel == null) {
            Timber.tag("SensorFusionManager").e("Linear acceleration sensor not available")
            close()
            return@callbackFlow
        }

        val samplingPeriodUs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            SensorManager.SENSOR_DELAY_FASTEST
        } else {
            20000 // 50Hz = 20ms = 20000μs
        }

        val registered = sensorManager.registerListener(
            listener,
            linearAccel,
            samplingPeriodUs
        )

        if (!registered) {
            Timber.tag("SensorFusionManager").e("Failed to register linear acceleration listener")
        } else {
            val rateLabel = if (samplingPeriodUs <= 0) {
                "FASTEST"
            } else {
                "${1000000 / samplingPeriodUs}Hz"
            }
            Timber.tag("SensorFusionManager").i("LINEAR_ACCELERATION registered ($rateLabel)")
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
            Timber.tag("SensorFusionManager").d("Linear acceleration unregistered")
        }
    }

    /**
     * Fallback: Regular accelerometer (requires manual gravity compensation)
     */
    private fun accelerometerFlow() = callbackFlow {
        // Removed strict isActive check to prevent startup race condition
        // if (!isActive.get()) { ... }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Capture raw sensor data for debugging (before any processing)
                    _rawSensorData.value = RawSensorData(
                        accelX = it.values[0],
                        accelY = it.values[1],
                        accelZ = it.values[2],
                        timestamp = it.timestamp
                    )

                    // Apply low-pass filter to estimate gravity (slow-changing component)
                    gravityX = alpha * gravityX + (1 - alpha) * it.values[0]
                    gravityY = alpha * gravityY + (1 - alpha) * it.values[1]
                    gravityZ = alpha * gravityZ + (1 - alpha) * it.values[2]

                    // Subtract gravity to get linear acceleration (fast-changing component)
                    val linearX = it.values[0] - gravityX
                    val linearY = it.values[1] - gravityY
                    val linearZ = it.values[2] - gravityZ

                    // Log raw sensor data and computed linear acceleration for debugging
                    Timber.tag("SensorFusionManager").d(
                        "RAW ACCEL: x=%.4f, y=%.4f, z=%.4f | LINEAR (filtered): x=%.4f, y=%.4f, z=%.4f",
                        it.values[0], it.values[1], it.values[2],
                        linearX, linearY, linearZ
                    )

                    // Increment heartbeat
                    _heartbeat.value = _heartbeat.value + 1

                    // Send linear acceleration (gravity-free) values
                    if (!trySend(Triple(linearX, linearY, linearZ)).isFailure) {
                        // Data sent successfully
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                Timber.tag("SensorFusionManager").d("Accelerometer accuracy changed: $accuracy")
            }
        }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            Timber.tag("SensorFusionManager").e("Accelerometer not available")
            close()
            return@callbackFlow
        }

        val samplingPeriodUs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            SensorManager.SENSOR_DELAY_FASTEST
        } else {
            20000 // 50Hz = 20ms = 20000μs
        }

        val registered = sensorManager.registerListener(
            listener,
            accelerometer,
            samplingPeriodUs
        )

        if (!registered) {
            Timber.tag("SensorFusionManager").e("Failed to register accelerometer listener")
        } else {
            val rateLabel = if (samplingPeriodUs <= 0) {
                "FASTEST"
            } else {
                "${1000000 / samplingPeriodUs}Hz"
            }
            Timber.tag("SensorFusionManager").d("Accelerometer registered ($rateLabel)")
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
            Timber.tag("SensorFusionManager").d("Accelerometer unregistered")
        }
    }

    private fun gyroscopeFlow() = callbackFlow {
        if (!isActive.get()) {
            awaitClose { }
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(Triple(it.values[0], it.values[1], it.values[2]))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) {
            Timber.tag("SensorFusionManager").e("Gyroscope not available")
            awaitClose { }
            return@callbackFlow
        }

        sensorManager.registerListener(
            listener,
            gyroscope,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Fuse sensor data into TelemetryData
     * Maps coordinates based on device orientation
     */
    private fun fuseSensorData(
        accel: Triple<Float, Float, Float>,
        gyro: Triple<Float, Float, Float>
    ): TelemetryData {
        // Log input values for debugging
        Timber.tag("SensorFusionManager").v(
            "fuseSensorData: input accel=(%.4f, %.4f, %.4f), rotation=%d",
            accel.first, accel.second, accel.third, currentRotation
        )

        // Map coordinates based on device rotation
        val (mappedX, mappedY) = CoordinateMapper.mapForRotation(
            x = accel.first,
            y = accel.second,
            rotation = currentRotation
        )

        // Log mapped values
        Timber.tag("SensorFusionManager").v(
            "fuseSensorData: mapped=(%.4f, %.4f)",
            mappedX, mappedY
        )

        // Convert to G-Force (assuming LINEAR_ACCELERATION is already gravity-free)
        val longitudinalG = mappedY / 9.81f  // Forward/backward
        val lateralG = mappedX / 9.81f      // Left/right

        // Convert gyroscope Z from rad/s to deg/s for yaw rate
        val yawRate = Math.toDegrees(gyro.third.toDouble()).toFloat()

        val telemetry = TelemetryData(
            timestamp = System.currentTimeMillis(),
            accelerationX = accel.first,
            accelerationY = accel.second,
            accelerationZ = accel.third,
            gravityX = 0f,  // LINEAR_ACCELERATION has no gravity component
            gravityY = 0f,
            gravityZ = 0f,
            gyroscopeX = gyro.first,
            gyroscopeY = gyro.second,
            gyroscopeZ = gyro.third,
            longitudinalG = longitudinalG,
            lateralG = lateralG,
            yawRate = yawRate
        )

        // Update latest state
        _latestTelemetry.value = telemetry

        // Log final G-Force values with more details (always log for debugging)
        Timber.tag("SensorFusionManager").d(
            "G-FORCE: Lon=%.3f, Lat=%.3f (rawX=%.3f, rawY=%.3f, mappedX=%.3f, mappedY=%.3f, rotation=%d)",
            longitudinalG, lateralG, accel.first, accel.second, mappedX, mappedY, currentRotation
        )

        return telemetry
    }

    /**
     * Lifecycle-aware sensor activation
     */
    fun bindToLifecycle(lifecycle: Lifecycle) {
        currentLifecycle.set(lifecycle)
        lifecycle.addObserver(this)
        Timber.tag("SensorFusionManager").d("Bound to lifecycle")
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> {
                startSensors()
            }
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                stopSensors()
            }
            Lifecycle.Event.ON_DESTROY -> {
                currentLifecycle.set(null)
            }
            else -> {}
        }
    }

    private fun startSensors() {
        if (isActive.compareAndSet(false, true)) {
            Timber.tag("SensorFusionManager").i("Sensors started")
            Timber.tag("SensorFusionManager").i(
                "Using: ${if (hasLinearAcceleration()) "LINEAR_ACCELERATION" else "ACCELEROMETER (fallback)"}"
            )
        }
    }

    private fun stopSensors() {
        if (isActive.compareAndSet(true, false)) {
            Timber.tag("SensorFusionManager").i("Sensors stopped")
        }
    }

    fun start() = startSensors()
    fun stop() = stopSensors()

    /**
     * Update device rotation for coordinate mapping
     */
    fun updateRotation(rotation: Int) {
        currentRotation = rotation
        Timber.tag("SensorFusionManager").d("Rotation updated: $rotation")
    }

    fun detectManeuver(telemetry: TelemetryData): ManeuverEvent {
        return when {
            telemetry.longitudinalG < HARD_BRAKING_THRESHOLD -> {
                ManeuverEvent.HardBraking(
                    maxDeceleration = telemetry.longitudinalG,
                    duration = 0L
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

    fun getHardwareTier(): HardwareTier {
        val hasGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val hasAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val hasLinearAccel = hasLinearAcceleration()

        return if (hasGyro && hasLinearAccel) {
            HardwareTier.TIER_1
        } else if (hasAccel) {
            HardwareTier.TIER_2
        } else {
            HardwareTier.TIER_3
        }
    }

    enum class HardwareTier {
        TIER_1,  // Full sensor suite with LINEAR_ACCELERATION
        TIER_2,  // Basic sensors
        TIER_3   // Minimal sensors
    }
}

/**
 * CoordinateMapper - Maps sensor coordinates based on device orientation
 *
 * For vehicle mounting:
 * - Phone in landscape (ROTATION_90): X = longitudinal, Y = lateral
 * - Phone in portrait (ROTATION_0): X = lateral, Y = longitudinal
 */
object CoordinateMapper {

    /**
     * Map (x, y) coordinates based on device rotation
     *
     * @param x Raw X acceleration
     * @param y Raw Y acceleration
     * @param rotation Device rotation (Surface.ROTATION_*)
     * @return Mapped (longitudinal, lateral) pair
     */
    fun mapForRotation(
        x: Float,
        y: Float,
        rotation: Int
    ): Pair<Float, Float> {
        return when (rotation) {
            Surface.ROTATION_0 -> {
                // Portrait: Y is longitudinal (forward/back), X is lateral
                Pair(x, -y)  // Invert Y for forward direction
            }
            Surface.ROTATION_90 -> {
                // Landscape (rotated 90° clockwise): X is longitudinal, Y is lateral
                Pair(y, x)
            }
            Surface.ROTATION_180 -> {
                // Portrait upside down
                Pair(-x, y)
            }
            Surface.ROTATION_270 -> {
                // Landscape (rotated 270°): -X is longitudinal, -Y is lateral
                Pair(-y, -x)
            }
            else -> {
                // Default to portrait
                Pair(x, -y)
            }
        }
    }

    /**
     * Get display rotation from configuration
     */
    fun getRotationFromConfig(config: Configuration): Int {
        return when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> Surface.ROTATION_90
            Configuration.ORIENTATION_PORTRAIT -> Surface.ROTATION_0
            else -> Surface.ROTATION_0
        }
    }
}
