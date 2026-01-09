package com.pacenote.vla.core.aibrain.platform

import android.graphics.Bitmap
import com.pacenote.vla.core.aibrain.VlaBrain
import kotlinx.coroutines.flow.Flow

/**
 * AiCoreClient - Android AICore Integration Interface
 *
 * This interface defines the contract for integrating with Android's AICore
 * (on-device AI platform for Gemini Nano and other SLMs).
 *
 * In v2.0, this serves as a placeholder for future implementation when AICore
 * becomes available on Android 16+ devices.
 *
 * AICore provides:
 * - System-level SLM management (no APK bloat)
 * - Hardware-accelerated inference (NPU/GPU)
 * - Automatic 16KB page alignment handling
 * - Low-latency on-device processing
 */
interface AiCoreClient {

    /**
     * Check if AICore is available on this device
     *
     * @return true if device supports AICore (requires Android 16+ and supported hardware)
     */
    suspend fun isAvailable(): Boolean

    /**
     * Initialize AICore with optional configuration
     *
     * @param config Optional configuration for AICore initialization
     * @return Result indicating success or failure
     */
    suspend fun initialize(config: AiCoreConfig? = null): Result<Unit>

    /**
     * Generate a text response using on-device SLM
     *
     * @param prompt The input prompt for the model
     * @param model The model to use (e.g., "gemini-nano", "phi-3-mini")
     * @return Flow of generated text tokens
     */
    fun generateResponse(
        prompt: String,
        model: String = DEFAULT_MODEL
    ): Flow<String>

    /**
     * Generate a multimodal response using vision + text
     *
     * @param bitmap The visual input (224x224 recommended)
     * @param telemetry Contextual sensor data
     * @param model The model to use
     * @return Flow of VlaAction responses
     */
    fun generateMultimodalResponse(
        bitmap: Bitmap,
        telemetry: VlaBrain.TelemetryContext?,
        model: String = DEFAULT_MODEL
    ): Flow<VlaBrain.VlaAction>

    /**
     * Get available models on this device
     *
     * @return List of model names supported by AICore
     */
    suspend fun getAvailableModels(): List<String>

    /**
     * Get model info including capabilities
     *
     * @param model The model name
     * @return AiCoreModelInfo or null if model not found
     */
    suspend fun getModelInfo(model: String): AiCoreModelInfo?

    /**
     * Check if a specific model is downloaded
     *
     * @param model The model name
     * @return true if model is available for use
     */
    suspend fun isModelDownloaded(model: String): Boolean

    /**
     * Download a model for on-device use
     *
     * @param model The model name
     * @param progress Callback for download progress (0.0 to 1.0)
     * @return Result indicating success or failure
     */
    suspend fun downloadModel(
        model: String,
        progress: ((Float) -> Unit)? = null
    ): Result<Unit>

    /**
     * Release AICore resources
     */
    fun release()

    companion object {
        const val DEFAULT_MODEL = "gemini-nano"
        const val FALLBACK_MODEL = "phi-3-mini"
    }
}

/**
 * Configuration for AICore initialization
 */
data class AiCoreConfig(
    val enableLogging: Boolean = true,
    val preferNpu: Boolean = true,  // Use NPU if available
    val maxTokens: Int = 512,       // Max tokens for generation
    val temperature: Float = 0.7f   // Sampling temperature
)

/**
 * Information about an AICore model
 */
data class AiCoreModelInfo(
    val name: String,
    val version: String,
    val sizeBytes: Long,
    val description: String,
    val capabilities: Set<ModelCapability>,
    val isDownloaded: Boolean = false
)

/**
 * Model capabilities
 */
enum class ModelCapability {
    TEXT_GENERATION,
    VISION_UNDERSTANDING,
    MULTIMODAL,
    FUNCTION_CALLING,
    STREAMING_RESPONSE
}

/**
 * AICore exception types
 */
sealed class AiCoreException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotAvailable(cause: Throwable? = null) : AiCoreException(
        "AICore is not available on this device", cause
    )
    class InitializationFailed(cause: Throwable) : AiCoreException(
        "Failed to initialize AICore", cause
    )
    class ModelNotFound(val model: String) : AiCoreException(
        "Model '$model' not found"
    )
    class InferenceFailed(cause: Throwable) : AiCoreException(
        "Inference failed", cause
    )
    class DownloadFailed(val model: String, cause: Throwable) : AiCoreException(
        "Failed to download model '$model'", cause
    )
}
