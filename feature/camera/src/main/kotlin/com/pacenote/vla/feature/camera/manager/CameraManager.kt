package com.pacenote.vla.feature.camera.manager

import android.content.Context
import android.graphics.Rect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.pacenote.vla.core.domain.model.RoiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera manager using CameraX with configurable ROI overlays
 * Handles both preview and frame analysis streams
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private val frameChannel = Channel<ImageProxy>(capacity = Channel.UNLIMITED)
    val frameFlow: Flow<ImageProxy> = frameChannel.receiveAsFlow()

    private var currentRoiConfig: RoiConfig = RoiConfig()
    private var isUsingBackCamera = true

    /**
     * Initialize camera with preview view
     */
    suspend fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        roiConfig: RoiConfig = RoiConfig()
    ): Result<Unit> {
        currentRoiConfig = roiConfig

        return try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider

            // Unbind any existing use cases
            provider.unbindAll()

            // Build preview use case
            previewUseCase = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Build analysis use case for frame processing
            analysisUseCase = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        ::analyzeFrame
                    )
                }

            // Bind to back camera (exterior facing)
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                previewUseCase,
                analysisUseCase
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize camera")
            Result.failure(e)
        }
    }

    private fun analyzeFrame(image: ImageProxy) {
        try {
            // Send frame to channel for processing
            frameChannel.trySend(image)
        } catch (e: Exception) {
            Timber.w(e, "Failed to send frame to channel")
            image.close()
        }
    }

    /**
     * Update ROI configuration
     */
    fun updateRoiConfig(config: RoiConfig) {
        currentRoiConfig = config
    }

    /**
     * Get current ROI configuration
     */
    fun getRoiConfig(): RoiConfig = currentRoiConfig

    /**
     * Calculate mirror ROI bounds based on image dimensions
     */
    fun getMirrorRoiBounds(imageWidth: Int, imageHeight: Int): Rect {
        val config = currentRoiConfig.mirrorRoi
        return Rect(
            (config.left * imageWidth).toInt(),
            (config.top * imageHeight).toInt(),
            (config.right * imageWidth).toInt(),
            (config.bottom * imageHeight).toInt()
        )
    }

    /**
     * Calculate windshield ROI bounds
     */
    fun getWindshieldRoiBounds(imageWidth: Int, imageHeight: Int): Rect {
        val config = currentRoiConfig.windshieldRoi
        return Rect(
            (config.left * imageWidth).toInt(),
            (config.top * imageHeight).toInt(),
            (config.right * imageWidth).toInt(),
            (config.bottom * imageHeight).toInt()
        )
    }

    /**
     * Toggle between front and back camera
     */
    suspend fun toggleCamera(lifecycleOwner: LifecycleOwner): Result<Unit> {
        val provider = cameraProvider ?: return Result.failure(IllegalStateException("Camera not initialized"))

        return try {
            provider.unbindAll()

            // Toggle camera
            isUsingBackCamera = !isUsingBackCamera
            val newSelector = if (isUsingBackCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            val useCases = listOfNotNull(previewUseCase, analysisUseCase)
            provider.bindToLifecycle(lifecycleOwner, newSelector, *useCases.toTypedArray())

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle camera")
            Result.failure(e)
        }
    }

    /**
     * Release camera resources
     */
    fun release() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        previewUseCase = null
        analysisUseCase = null
        frameChannel.close()
    }
}
