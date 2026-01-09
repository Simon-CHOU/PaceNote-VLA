package com.pacenote.vla.feature.vision

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.pacenote.vla.core.aibrain.VlaBrain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VisionManager - Pure Edge Vision Processing
 *
 * Handles CameraX frame capture and routes to VlaBrain for on-device processing.
 * No cloud dependencies, no MediaPipe alignment issues.
 */
@Singleton
class VisionManager @Inject constructor(
    // @ApplicationContext private val context: Context,
    private val vlaBrain: VlaBrain
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _aiActionFlow = MutableStateFlow<VlaBrain.VlaAction?>(null)
    val aiActionFlow: Flow<VlaBrain.VlaAction?> = _aiActionFlow.asStateFlow()

    private val _statusFlow = MutableStateFlow<VisionStatus>(VisionStatus.Idle)
    val statusFlow: Flow<VisionStatus> = _statusFlow.asStateFlow()

    private var frameCount = 0
    private var lastProcessedTime = 0L

    sealed class VisionStatus {
        data object Idle : VisionStatus()
        data object Processing : VisionStatus()
        data class Error(val message: String) : VisionStatus()
    }

    /**
     * Process a frame from CameraX
     * Converts to Bitmap and sends to VlaBrain
     */
    suspend fun processFrame(imageProxy: ImageProxy, telemetry: VlaBrain.TelemetryContext?) {
        _statusFlow.value = VisionStatus.Processing

        try {
            // Rate limiting: max 5 FPS for AI processing to save battery
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProcessedTime < 200) {
                imageProxy.close()
                return
            }
            lastProcessedTime = currentTime

            // Convert ImageProxy to Bitmap
            val bitmap = withContext(Dispatchers.Default) {
                imageProxyToBitmap(imageProxy)
            }

            if (bitmap != null) {
                val vlaFrame = VlaBrain.VlaFrame(
                    bitmap = bitmap,
                    timestamp = imageProxy.imageInfo.timestamp,
                    telemetry = telemetry
                )

                // Process with VlaBrain
                vlaBrain.processFrame(vlaFrame)
                    .onEach { action ->
                        _aiActionFlow.value = action
                        Timber.tag("VisionManager").d(
                            "AI Action: ${action.alert} - ${action.message} (${action.confidence})"
                        )
                    }
                    .launchIn(scope)

                frameCount++
            }

        } catch (e: Exception) {
            Timber.tag("VisionManager").e(e, "Frame processing failed")
            _statusFlow.value = VisionStatus.Error(e.message ?: "Unknown error")
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Convert CameraX ImageProxy to Bitmap
     * Handles YUV_420_888 format from back camera
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer // Y
            val uBuffer = image.planes[1].buffer // U
            val vBuffer = image.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                ImageFormat.NV21,
                image.width,
                image.height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val imageBytes = out.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Timber.tag("VisionManager").e(e, "Bitmap conversion failed")
            null
        }
    }

    /**
     * Initialize the VlaBrain AI
     */
    suspend fun initialize(apiKey: String? = null): Result<Unit> {
        return vlaBrain.initialize(apiKey)
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
     * Release resources
     */
    fun release() {
        vlaBrain.release()
        _statusFlow.value = VisionStatus.Idle
    }
}
