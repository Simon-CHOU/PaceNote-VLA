package com.pacenote.vla.feature.telemetry.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.pacenote.vla.core.domain.model.GForceVector
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// Define HUD colors locally since theme colors are not available
private val HudBackground = Color(0xFF1A1A1A)
private val HudForeground = Color(0xFFE0E0E0)
private val GForcePositive = Color(0xFF00E676)  // Green for acceleration
private val GForceNegative = Color(0xFFFF5252)  // Red for braking
private val GForceLateral = Color(0xFF2979FF)   // Blue for cornering

/**
 * WRC-style G-Force Friction Circle HUD
 * Shows lateral and longitudinal G-forces in real-time
 */
@Composable
fun GForceFrictionCircle(
    gForce: GForceVector,
    maxG: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val size = 200.dp

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val radius = size.toPx() / 2 - 8.dp.toPx()

        // Background circle
        drawCircle(
            color = HudBackground,
            radius = radius,
            center = center
        )

        // Grid circles (0.25g, 0.5g, 0.75g, 1.0g)
        val gridSteps = listOf(0.25f, 0.5f, 0.75f, 1.0f)
        gridSteps.forEach { step ->
            drawCircle(
                color = HudForeground.copy(alpha = 0.2f),
                radius = radius * step,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Crosshairs
        drawLine(
            color = HudForeground.copy(alpha = 0.3f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = HudForeground.copy(alpha = 0.3f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Axis labels
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.apply {
            // Labels for G values
            drawText("1.0G", center.x, center.y - radius - 4.dp.toPx(), textPaint)
            drawText("-1.0G", center.x, center.y + radius + 20.dp.toPx(), textPaint)
            drawText("LAT", center.x - radius - 20.dp.toPx(), center.y + 8.dp.toPx(), textPaint)
            drawText("LON", center.x + radius + 20.dp.toPx(), center.y + 8.dp.toPx(), textPaint)
        }

        // Draw G-Force vector
        if (gForce.magnitude > 0.01f) {
            val normalizedMagnitude = min(gForce.magnitude / maxG, 1.0f)
            val vectorLength = radius * normalizedMagnitude

            // Calculate angle from lateral/longitudinal
            // Note: In canvas, Y is down (positive), but in physics, up is positive
            // So we flip the Y axis
            val angle = atan2(-gForce.longitudinal, gForce.lateral)

            val endPoint = Offset(
                x = center.x + (vectorLength * cos(angle)).toFloat(),
                y = center.y + (vectorLength * sin(angle)).toFloat()
            )

            // Color based on G type
            val vectorColor = when {
                abs(gForce.longitudinal) > abs(gForce.lateral) -> {
                    if (gForce.longitudinal > 0) GForcePositive else GForceNegative
                }
                else -> GForceLateral
            }

            // Draw vector line
            drawLine(
                color = vectorColor,
                start = center,
                end = endPoint,
                strokeWidth = 4.dp.toPx()
            )

            // Draw endpoint
            drawCircle(
                color = vectorColor,
                radius = 6.dp.toPx(),
                center = endPoint
            )

            // Draw magnitude text
            val magnitudeText = String.format("%.2fG", gForce.magnitude)
            val magPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 32f
                isAntiAlias = true
                setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    magnitudeText,
                    center.x,
                    center.y + radius + 40.dp.toPx(),
                    magPaint
                )
            }
        }

        // Outer ring
        drawCircle(
            color = HudForeground.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Compact G-Force bar indicator
 */
@Composable
fun GForceBarIndicator(
    gForce: GForceVector,
    modifier: Modifier = Modifier
) {
    // TODO: Implement horizontal/vertical bar indicators
    // for smaller screens where friction circle is too large
}
