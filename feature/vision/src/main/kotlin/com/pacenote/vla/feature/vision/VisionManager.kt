package com.pacenote.vla.feature.vision

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.pacenote.vla.core.aibrain.VlaBrain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VisionManager - Pure Edge Vision Processing v2.0
 *
 * Manages the CameraX frame capture pipeline and routes to VlaBrain
 * for on-device AI inference. Now includes:
 * - Frame analysis with 224x224 downsampling
 * - End-to-end latency tracking
 * - Red pixel detection for mock reasoning
 */
@Singleton
class VisionManager @Inject constructor(
    private val vlaBrain: VlaBrain
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // AI action flow
    private val _aiActionFlow = MutableStateFlow<VlaBrain.VlaAction?>(null)
    val aiActionFlow: StateFlow<VlaBrain.VlaAction?> = _aiActionFlow.asStateFlow()

    // Status tracking
    private val _statusFlow = MutableStateFlow<VisionStatus>(VisionStatus.Idle)
    val statusFlow: StateFlow<VisionStatus> = _statusFlow.asStateFlow()

    // Latency metrics
    private val _latencyMs = MutableStateFlow(0L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val totalLatency = AtomicLong(0)
    private var processedFrameCount = 0

    // Frame analyzer with latency tracking
    val frameAnalyzer = VlaFrameAnalyzer { bitmap, latency, telemetry ->
        onFrameAnalyzed(bitmap, latency, telemetry)
    }

    sealed class VisionStatus {
        data object Idle : VisionStatus()
        data object Processing : VisionStatus()
        data class Error(val message: String) : VisionStatus()
    }

    /**
     * Process a frame from CameraX
     * NOTE: With the new VlaFrameAnalyzer, frames are processed directly
     * by the analyzer. This method is kept for backward compatibility.
     */
    suspend fun processFrame(imageProxy: ImageProxy, telemetry: VlaBrain.TelemetryContext?) {
        _statusFlow.value = VisionStatus.Processing

        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)

            if (bitmap != null) {
                val vlaFrame = VlaBrain.VlaFrame(
                    bitmap = bitmap,
                    timestamp = imageProxy.imageInfo.timestamp,
                    telemetry = telemetry
                )

                // Process with VlaBrain
                processWithVlaBrain(vlaFrame)
            } else {
                Timber.tag("VisionManager").w("Failed to convert frame to bitmap")
            }

        } catch (e: Exception) {
            Timber.tag("VisionManager").e(e, "Frame processing failed")
            _statusFlow.value = VisionStatus.Error(e.message ?: "Unknown error")
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Callback from VlaFrameAnalyzer when a frame is analyzed
     */
    private fun onFrameAnalyzed(
        bitmap: Bitmap,
        latencyMs: Long,
        telemetry: VlaBrain.TelemetryContext?
    ) {
        // Update latency metrics
        _latencyMs.value = latencyMs
        totalLatency.addAndGet(latencyMs)
        processedFrameCount++

        // Create VlaFrame and process
        val vlaFrame = VlaBrain.VlaFrame(
            bitmap = bitmap,
            timestamp = System.currentTimeMillis(),
            telemetry = telemetry
        )

        // Process with VlaBrain
        processWithVlaBrain(vlaFrame)
    }

    /**
     * Process frame with VlaBrain
     */
    private fun processWithVlaBrain(frame: VlaBrain.VlaFrame) {
        scope.launch {
            try {
                vlaBrain.processFrame(frame).collect { action ->
                    _aiActionFlow.value = action
                    Timber.tag("VisionManager").d(
                        "AI Action: ${action.alert} - ${action.message} (${action.confidence})"
                    )
                }
            } catch (e: Exception) {
                Timber.tag("VisionManager").e(e, "VlaBrain processing failed")
                _statusFlow.value = VisionStatus.Error(e.message ?: "VlaBrain error")
            }
        }
    }

    /**
     * Convert ImageProxy to Bitmap (legacy method for backward compatibility)
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            android.graphics.ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, image.width, image.height),
            90,
            out
        )
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * Get current AI status from VlaBrain
     */
    fun getAiStatus(): VlaBrain.AiStatus = vlaBrain.getStatus()

    /**
     * Check if AI is ready
     */
    fun isReady(): Boolean = vlaBrain.isReady()

    /**
     * Get average latency across all processed frames
     */
    fun getAverageLatency(): Long {
        return if (processedFrameCount > 0) {
            totalLatency.get() / processedFrameCount
        } else {
            0
        }
    }

    /**
     * Get performance status string
     */
    fun getPerformanceStatus(): String {
        val avgLatency = getAverageLatency()
        return when {
            avgLatency == 0L -> "No frames processed"
            avgLatency < 50 -> "✓ Excellent (${avgLatency}ms < 50ms target)"
            avgLatency < 100 -> "⚠ Good (${avgLatency}ms)"
            else -> "✗ Slow (${avgLatency}ms > 100ms)"
        }
    }

    /**
     * Initialize the VlaBrain AI
     */
    suspend fun initialize(apiKey: String? = null): Result<Unit> {
        return vlaBrain.initialize(apiKey)
    }

    /**
     * Release resources
     */
    fun release() {
        vlaBrain.release()
        _statusFlow.value = VisionStatus.Idle
        Timber.tag("VisionManager").d(
            "Performance summary: ${processedFrameCount} frames, " +
            "avg latency: ${getAverageLatency()}ms"
        )
    }
}
