package com.pacenote.vla.feature.camera.ui

import android.graphics.Rect
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.pacenote.vla.core.domain.model.RoiConfig

// Color constants if not available in theme
private val HudAccent = Color(0xFF00D4FF) // Cyan for HUD accent
private val GForceLateral = Color(0xFFFF6B00) // Orange for lateral G-force

/**
 * Camera preview with ROI overlay
 */
@Composable
fun CameraPreviewWithRoi(
    modifier: Modifier = Modifier,
    roiConfig: RoiConfig = RoiConfig(),
    onCameraReady: (PreviewView) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier = modifier) {
        // Camera Preview
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    onCameraReady(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ROI Overlay
        previewView?.let { preview ->
            RoiOverlay(
                previewView = preview,
                roiConfig = roiConfig,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun RoiOverlay(
    previewView: PreviewView,
    roiConfig: RoiConfig,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Mirror ROI (blind spot detection area)
        val mirrorRoi = roiConfig.mirrorRoi
        drawRect(
            color = HudAccent,
            style = Stroke(width = 4.dp.toPx()),
            topLeft = Offset(
                x = mirrorRoi.left * width,
                y = mirrorRoi.top * height
            ),
            size = androidx.compose.ui.geometry.Size(
                width = (mirrorRoi.right - mirrorRoi.left) * width,
                height = (mirrorRoi.bottom - mirrorRoi.top) * height
            )
        )

        // Windshield ROI (forward vision area)
        val windshieldRoi = roiConfig.windshieldRoi
        drawRect(
            color = GForceLateral.copy(alpha = 0.5f),
            style = Stroke(width = 2.dp.toPx()),
            topLeft = Offset(
                x = windshieldRoi.left * width,
                y = windshieldRoi.top * height
            ),
            size = androidx.compose.ui.geometry.Size(
                width = (windshieldRoi.right - windshieldRoi.left) * width,
                height = (windshieldRoi.bottom - windshieldRoi.top) * height
            )
        )

        // Labels
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 24f
            isAntiAlias = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }

        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "BLIND SPOT",
                mirrorRoi.left * width + 8.dp.toPx(),
                mirrorRoi.top * height + 32.dp.toPx(),
                textPaint
            )
            drawText(
                "FORWARD VISION",
                windshieldRoi.left * width + 8.dp.toPx(),
                windshieldRoi.top * height + 32.dp.toPx(),
                textPaint
            )
        }
    }
}

/**
 * Adjustable ROI selector for settings
 */
@Composable
fun RoiAdjuster(
    roiConfig: RoiConfig,
    onRoiChanged: (RoiConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Implement gesture-based ROI adjustment
    // This will allow users to drag corners to adjust detection zones
}
