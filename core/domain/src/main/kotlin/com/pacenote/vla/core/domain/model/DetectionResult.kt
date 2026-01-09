package com.pacenote.vla.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Object detection result from MediaPipe
 */
@Serializable
data class DetectionResult(
    val timestamp: Long = System.currentTimeMillis(),
    val objects: List<DetectedObject> = emptyList(),
    val processingTimeMs: Long = 0L
) {
    val hasBlindSpotDetection: Boolean
        get() = objects.any { it.isInBlindSpot }

    companion object {
        val EMPTY = DetectionResult()
    }
}

@Serializable
data class DetectedObject(
    val className: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val isInBlindSpot: Boolean = false
)

@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Reflex alert from local edge processing
 */
@Serializable
sealed class ReflexAlert {
    @Serializable
    data object None : ReflexAlert()

    @Serializable
    data class BlindSpotObject(
        val objectClass: String,
        val confidence: Float,
        val position: String
    ) : ReflexAlert()

    @Serializable
    data class CriticalManeuver(
        val gForce: Float,
        val maneuverType: String
    ) : ReflexAlert()
}
