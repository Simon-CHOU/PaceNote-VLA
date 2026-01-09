package com.pacenote.vla.feature.telemetry.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Define HUD colors locally (same as in GForceMeter.kt)
private val HudBackground = Color(0xFF1A1A1A)
private val HudForeground = Color(0xFFE0E0E0)
private val GForceLateral = Color(0xFF2979FF)
private val HudAccent = Color(0xFFFF6B00)

/**
 * WRC-style digital speedometer
 * Shows speed in km/h with large, clear digits
 */
@Composable
fun DigitalSpeedometer(
    speedMps: Float,
    modifier: Modifier = Modifier
) {
    val speedKmh = (speedMps * 3.6f).toInt()

    Surface(
        modifier = modifier,
        color = HudBackground
    ) {
        Canvas(modifier = Modifier.size(200.dp, 80.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Draw speed value
            val speedText = String.format("%03d", speedKmh)
            val unitText = "KM/H"

            val speedPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 64f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(
                    "sans-serif-condensed",
                    android.graphics.Typeface.BOLD
                )
                setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val unitPaint = android.graphics.Paint().apply {
                color = HudAccent.hashCode()
                textSize = 20f
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(
                    "sans-serif-condensed",
                    android.graphics.Typeface.BOLD
                )
                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                textAlign = android.graphics.Paint.Align.CENTER
            }

            drawContext.canvas.nativeCanvas.apply {
                // Speed value
                drawText(
                    speedText,
                    centerX,
                    centerY + 20f,
                    speedPaint
                )

                // Unit
                drawText(
                    unitText,
                    centerX,
                    centerY + 55f,
                    unitPaint
                )
            }
        }
    }
}

/**
 * Analog-style speedometer with arc
 */
@Composable
fun AnalogSpeedometer(
    speedMps: Float,
    maxSpeed: Float = 50f, // ~180 km/h
    modifier: Modifier = Modifier
) {
    val size = 180.dp

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2, size.toPx() / 2)
        val radius = size.toPx() / 2 - 8.dp.toPx()

        // Background arc (270 degrees, starting from 135°)
        val startAngle = 135f
        val sweepAngle = 270f

        drawArc(
            color = HudBackground,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = 16.dp.toPx()),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )

        // Speed arc (colored portion)
        val normalizedSpeed = (speedMps / maxSpeed).coerceIn(0f, 1f)
        val currentSweep = sweepAngle * normalizedSpeed

        // Color based on speed (green -> yellow -> red)
        val speedColor = when {
            normalizedSpeed < 0.5f -> Color(0xFF00E676)
            normalizedSpeed < 0.75f -> Color(0xFFFFAB00)
            else -> Color(0xFFFF1744)
        }

        drawArc(
            color = speedColor,
            startAngle = startAngle,
            sweepAngle = currentSweep,
            useCenter = false,
            style = Stroke(width = 16.dp.toPx()),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )

        // Speed value text
        val speedKmh = (speedMps * 3.6f).toInt()
        val speedText = String.format("%d", speedKmh)

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(
                "sans-serif-condensed",
                android.graphics.Typeface.BOLD
            )
            setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val unitPaint = android.graphics.Paint().apply {
            color = HudAccent.hashCode()
            textSize = 16f
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.apply {
            drawText(speedText, center.x, center.y + 15f, textPaint)
            drawText("KM/H", center.x, center.y + 45f, unitPaint)
        }

        // Tick marks
        val tickPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            strokeWidth = 2f
            isAntiAlias = true
        }

        for (i in 0..10) {
            val angle = Math.toRadians((startAngle + (sweepAngle * i / 10)).toDouble())
            val innerRadius = radius - 24.dp.toPx()
            val outerRadius = radius - 12.dp.toPx()

            val x1 = center.x + (innerRadius * cos(angle)).toFloat()
            val y1 = center.y + (innerRadius * sin(angle)).toFloat()
            val x2 = center.x + (outerRadius * cos(angle)).toFloat()
            val y2 = center.y + (outerRadius * sin(angle)).toFloat()

            drawContext.canvas.nativeCanvas.apply {
                drawLine(x1, y1, x2, y2, tickPaint)
            }
        }
    }
}
