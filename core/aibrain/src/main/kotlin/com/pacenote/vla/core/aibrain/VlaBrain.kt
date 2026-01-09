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
            // Check telemetry for immediate hazards
            val telemetry = frame.telemetry
            val alertLevel = when {
                telemetry != null && telemetry.gForceLongitudinal < -0.4f -> AlertLevel.HIGH
                telemetry != null && abs(telemetry.gForceLateral) > 0.4f -> AlertLevel.MEDIUM
                else -> AlertLevel.LOW
            }

            // For demo purposes, return a simple action
            // In production, this would call the generative model
            val message = when (alertLevel) {
                AlertLevel.HIGH -> "Hard braking detected - preparing corner entry analysis"
                AlertLevel.MEDIUM -> "Sharp turn in progress - monitoring apex"
                AlertLevel.LOW -> "Monitoring driving conditions"
                else -> "System ready"
            }

            VlaAction(
                alert = alertLevel,
                message = message,
                tts = if (alertLevel != AlertLevel.NONE) message else null,
                confidence = 0.85f,
                timestamp = frame.timestamp
            )
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
