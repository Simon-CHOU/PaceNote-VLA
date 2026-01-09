package com.pacenote.vla.core.aibrain

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * VlaBrain - Pure Edge On-Device AI Core
 *
 * Manages the Small Language Model (SLM) for Vision-Language-Action processing.
 * Designed to work with Android AICore / Gemini Nano on-device.
 */
@Singleton
class VlaBrain @Inject constructor(
    // @ApplicationContext private val context: Context
) {
    private var generativeModel: GenerativeModel? = null
    private val frameBuffer = ConcurrentLinkedQueue<VlaFrame>()
    private var isInitialized = false
    private var initializationStatus: AiStatus = AiStatus.Idle

    /**
     * Represents a single vision frame for processing
     */
    data class VlaFrame(
        val bitmap: Bitmap,
        val timestamp: Long,
        val telemetry: TelemetryContext? = null
    )

    /**
     * Sensor telemetry context for multimodal input
     */
    data class TelemetryContext(
        val gForceLongitudinal: Float,
        val gForceLateral: Float,
        val speed: Float? = null,
        val yawRate: Float? = null
    )

    /**
     * AI action output in JSON format
     */
    data class VlaAction(
        val alert: AlertLevel,
        val message: String,
        val tts: String? = null,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class AlertLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /**
     * AI initialization status
     */
    sealed class AiStatus {
        data object Idle : AiStatus()
        data object Initializing : AiStatus()
        data class Ready(val modelName: String) : AiStatus()
        data class Error(val message: String) : AiStatus()
    }

    /**
     * Initialize the on-device AI model
     * For v2.0, we use a stub that's ready for AICore integration
     */
    suspend fun initialize(apiKey: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            initializationStatus = AiStatus.Initializing
            Timber.tag("VlaBrain").d("Initializing on-device AI...")

            // TODO: Integrate with Android AICore when available
            // For now, create a mock model for testing
            generativeModel = if (apiKey != null) {
                GenerativeModel(
                    modelName = "gemini-2.0-flash-exp",
                    apiKey = apiKey
                )
            } else {
                // Stub for pure edge mode (will use AICore in production)
                null
            }

            initializationStatus = AiStatus.Ready(
                modelName = if (apiKey != null) "Gemini 2.0 Flash (API)" else "AICore Stub (v2.0)"
            )
            isInitialized = true

            Timber.tag("VlaBrain").d("AI initialized: ${initializationStatus}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("VlaBrain").e(e, "Failed to initialize AI")
            initializationStatus = AiStatus.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Process a vision frame and generate VLA action
     * Returns a Flow of actions for continuous processing
     */
    fun processFrame(frame: VlaFrame): Flow<VlaAction> = flow {
        if (!isInitialized) {
            emit(VlaAction(
                alert = AlertLevel.NONE,
                message = "AI not initialized",
                tts = null,
                confidence = 0f
            ))
            return@flow
        }

        // Add to buffer for processing
        frameBuffer.offer(frame)

        // Keep buffer manageable (max 10 frames)
        while (frameBuffer.size > 10) {
            val oldFrame = frameBuffer.poll()
            oldFrame?.bitmap?.recycle()
        }

        // Process the frame
        val action = analyzeFrame(frame)
        emit(action)
    }

    /**
     * Analyze a single frame using the AI model
     * For v2.0 pure edge, returns mock output
     */
    private suspend fun analyzeFrame(frame: VlaFrame): VlaAction = withContext(Dispatchers.IO) {
        try {
            // Detect red pixels in the bitmap (for brake light detection)
            val redPixelRatio = detectRedPixels(frame.bitmap)

            // Use processContext for full analysis
            processContext(frame.bitmap, redPixelRatio, frame.telemetry)
        } catch (e: Exception) {
            Timber.tag("VlaBrain").e(e, "Frame analysis failed")
            VlaAction(
                alert = AlertLevel.NONE,
                message = "Analysis error: ${e.message}",
                tts = null,
                confidence = 0f
            )
        }
    }

    /**
     * Process context with vision and telemetry data
     * This is the main entry point for VLA reasoning
     *
     * @param bitmap The captured frame (224x224)
     * @param redPixelRatio Ratio of red pixels detected (0.0 to 1.0)
     * @param telemetry Current sensor telemetry context
     * @return VlaAction with alert level and message
     */
    fun processContext(
        bitmap: Bitmap,
        redPixelRatio: Float = 0f,
        telemetry: TelemetryContext? = null
    ): VlaAction {
        val lateralG = telemetry?.gForceLateral ?: 0f
        val longitudinalG = telemetry?.gForceLongitudinal ?: 0f

        // Mock reasoning: Detect heavy braking
        // Condition: High lateral G (>0.5) AND significant red pixels (>1%)
        // This simulates detecting brake lights during hard cornering
        val isHeavyBraking = abs(lateralG) > 0.5f && redPixelRatio > 0.01f

        val alertLevel = when {
            isHeavyBraking -> AlertLevel.CRITICAL
            longitudinalG < -0.4f -> AlertLevel.HIGH
            abs(lateralG) > 0.4f -> AlertLevel.MEDIUM
            redPixelRatio > 0.02f -> AlertLevel.LOW  // Brake lights detected
            else -> AlertLevel.NONE
        }

        val message = when {
            isHeavyBraking -> "HEAVY BRAKING DETECTED - Red pixels: ${(redPixelRatio * 100).toInt()}%, Lateral G: ${String.format("%.2f", abs(lateralG))}"
            alertLevel == AlertLevel.HIGH -> "Hard braking detected - G: ${String.format("%.2f", longitudinalG)}"
            alertLevel == AlertLevel.MEDIUM -> "Sharp turn in progress - Lateral G: ${String.format("%.2f", lateralG)}"
            alertLevel == AlertLevel.LOW -> "Brake lights detected - Ratio: ${(redPixelRatio * 100).toInt()}%"
            else -> "System monitoring - All parameters normal"
        }

        val tts = when {
            alertLevel >= AlertLevel.HIGH -> message
            else -> null
        }

        return VlaAction(
            alert = alertLevel,
            message = message,
            tts = tts,
            confidence = when {
                isHeavyBraking -> 0.95f
                alertLevel == AlertLevel.HIGH -> 0.90f
                alertLevel == AlertLevel.MEDIUM -> 0.80f
                alertLevel == AlertLevel.LOW -> 0.70f
                else -> 0.60f
            },
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Detect red pixels in a bitmap
     * Used for brake light detection in mock reasoning
     *
     * @param bitmap The image to analyze
     * @return Ratio of red pixels (0.0 to 1.0)
     */
    private fun detectRedPixels(bitmap: Bitmap): Float {
        var redPixelCount = 0
        val totalPixels = bitmap.width * bitmap.height

        // Sample every 10th pixel for performance
        val stride = 10

        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            for (i in pixels.indices step stride) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Check if pixel is predominantly red
                // Red pixel criteria: R > 150 and R > G * 1.5 and R > B * 1.5
                if (r > 150 && r > g * 1.5f && r > b * 1.5f) {
                    redPixelCount++
                }
            }
        } catch (e: Exception) {
            Timber.tag("VlaBrain").e(e, "Red pixel detection failed")
        }

        val sampledPixels = totalPixels / stride
        return if (sampledPixels > 0) {
            redPixelCount.toFloat() / sampledPixels
        } else {
            0f
        }
    }

    /**
     * Process with full generative model (when API key available)
     */
    private suspend fun processWithGenerativeModel(frame: VlaFrame): VlaAction {
        val model = generativeModel ?: return VlaAction(
            alert = AlertLevel.NONE,
            message = "Model not available",
            tts = null,
            confidence = 0f
        )

        return try {
            val prompt = buildPrompt(frame)
            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            // Parse response into action
            parseResponse(response.text ?: "No response")
        } catch (e: Exception) {
            Timber.tag("VlaBrain").e(e, "Generative model failed")
            VlaAction(
                alert = AlertLevel.NONE,
                message = "Model error: ${e.message}",
                tts = null,
                confidence = 0f
            )
        }
    }

    private fun buildPrompt(frame: VlaFrame): String {
        val telemetry = frame.telemetry
        return """
            Analyze this driving scene and provide racing pacenotes.

            Telemetry:
            - Longitudinal G: ${telemetry?.gForceLongitudinal ?: "N/A"}
            - Lateral G: ${telemetry?.gForceLateral ?: "N/A"}
            - Speed: ${telemetry?.speed ?: "N/A"} km/h

            Provide JSON response with:
            - alert: NONE/LOW/MEDIUM/HIGH/CRITICAL
            - message: Brief racing note
            - tts: Text-to-speech version
        """.trimIndent()
    }

    private fun parseResponse(text: String): VlaAction {
        // Simple parsing - in production would use proper JSON parsing
        return VlaAction(
            alert = AlertLevel.LOW,
            message = text.take(100),
            tts = text.take(50),
            confidence = 0.75f
        )
    }

    /**
     * Get current AI status
     */
    fun getStatus(): AiStatus = initializationStatus

    /**
     * Check if ready for processing
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Release resources
     */
    fun release() {
        frameBuffer.forEach { it.bitmap.recycle() }
        frameBuffer.clear()
        generativeModel = null
        isInitialized = false
        initializationStatus = AiStatus.Idle
    }
}
