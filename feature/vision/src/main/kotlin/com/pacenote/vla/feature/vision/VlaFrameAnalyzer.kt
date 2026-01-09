package com.pacenote.vla.feature.vision

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.pacenote.vla.core.aibrain.VlaBrain
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/**
 * VlaFrameAnalyzer - Pure Edge Vision Processing
 *
 * Implements ImageAnalysis.Analyzer to process CameraX frames
 * and pass them to VlaBrain for on-device AI inference.
 *
 * Features:
 * - 224x224 downsampling (standard SLM input size)
 * - End-to-end latency tracking
 * - Red pixel detection for mock reasoning
 */
class VlaFrameAnalyzer(
    private val onFrameProcessed: (Bitmap, Long, VlaBrain.TelemetryContext?) -> Unit
) : ImageAnalysis.Analyzer {

    // Target resolution for SLM input (standard size: 224x224)
    private val TARGET_WIDTH = 224
    private val TARGET_HEIGHT = 224

    // Performance metrics
    private var frameCount = 0
    private var totalLatencyMs = 0L
    private var averageLatencyMs = 0L

    /**
     * Analyze a single frame from CameraX
     * Note: Return type is Unit as required by ImageAnalysis.Analyzer interface
     */
    override fun analyze(image: ImageProxy) {
        val startTime = System.nanoTime()

        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(image, TARGET_WIDTH, TARGET_HEIGHT)

            if (bitmap != null) {
                // Calculate processing latency
                val latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
                updateLatencyMetrics(latencyMs)

                // Detect red pixels (for mock reasoning)
                val redPixelRatio = detectRedPixels(bitmap)

                // Callback with processed frame
                // Note: TelemetryContext will be provided by VisionManager
                onFrameProcessed(bitmap, latencyMs, null)

                Timber.tag("VlaFrameAnalyzer").v(
                    "Frame processed: ${bitmap.width}x${bitmap.height}, " +
                    "latency: ${latencyMs}ms, red: ${(redPixelRatio * 100).toInt()}%"
                )

                frameCount++
            } else {
                Timber.tag("VlaFrameAnalyzer").w("Failed to convert frame to bitmap")
            }
        } catch (e: Exception) {
            Timber.tag("VlaFrameAnalyzer").e(e, "Frame analysis failed")
        } finally {
            // Important: Close the image to free up camera resources
            image.close()
        }
    }

    /**
     * Convert CameraX ImageProxy to Bitmap with optional resizing
     *
     * @param image The ImageProxy from CameraX
     * @param targetWidth Target width (use 0 for original)
     * @param targetHeight Target height (use 0 for original)
     * @return Bitmap or null if conversion fails
     */
    private fun imageProxyToBitmap(
        image: ImageProxy,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer // Y
            val uBuffer = image.planes[1].buffer // U
            val vBuffer = image.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped in NV21 format
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
            yuvImage.compressToJpeg(
                Rect(0, 0, image.width, image.height),
                90, // JPEG quality
                out
            )

            val imageBytes = out.toByteArray()
            val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(
                imageBytes,
                0,
                imageBytes.size
            )

            // Resize if target dimensions specified
            if (targetWidth > 0 && targetHeight > 0 && originalBitmap != null) {
                val resizedBitmap = Bitmap.createScaledBitmap(
                    originalBitmap,
                    targetWidth,
                    targetHeight,
                    true // Filter for better quality
                )

                // Rotate if needed (CameraX images may be rotated)
                val rotationDegrees = image.imageInfo.rotationDegrees
                if (rotationDegrees != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDegrees.toFloat())
                    val rotatedBitmap = Bitmap.createBitmap(
                        resizedBitmap,
                        0,
                        0,
                        resizedBitmap.width,
                        resizedBitmap.height,
                        matrix,
                        true
                    )
                    resizedBitmap.recycle()
                    rotatedBitmap
                } else {
                    resizedBitmap
                }
            } else {
                originalBitmap
            }
        } catch (e: Exception) {
            Timber.tag("VlaFrameAnalyzer").e(e, "Bitmap conversion failed")
            null
        }
    }

    /**
     * Detect red pixels in the bitmap
     * Used for mock reasoning (simulating brake light detection)
     *
     * @return Ratio of red pixels (0.0 to 1.0)
     */
    fun detectRedPixels(bitmap: Bitmap): Float {
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
            Timber.tag("VlaFrameAnalyzer").e(e, "Red pixel detection failed")
        }

        val sampledPixels = totalPixels / stride
        return if (sampledPixels > 0) {
            redPixelCount.toFloat() / sampledPixels
        } else {
            0f
        }
    }

    /**
     * Update latency metrics
     */
    private fun updateLatencyMetrics(latencyMs: Long) {
        totalLatencyMs += latencyMs
        frameCount++

        // Update average every 10 frames
        if (frameCount % 10 == 0) {
            averageLatencyMs = totalLatencyMs / frameCount
            Timber.tag("VlaFrameAnalyzer").i(
                "Performance: avg latency ${averageLatencyMs}ms, " +
                "target: <50ms, status: ${if (averageLatencyMs < 50) "✓" else "✗"}"
            )
        }
    }

    /**
     * Get average latency in milliseconds
     */
    fun getAverageLatency(): Long = averageLatencyMs

    /**
     * Get number of frames processed
     */
    fun getFrameCount(): Int = frameCount

    /**
     * Reset metrics
     */
    fun resetMetrics() {
        frameCount = 0
        totalLatencyMs = 0
        averageLatencyMs = 0
    }
}
