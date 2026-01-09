package com.pacenote.vla.feature.telemetry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacenote.vla.feature.livekit.client.ConnectionState
import com.pacenote.vla.feature.livekit.ui.ConnectionStatusBar
import com.pacenote.vla.feature.telemetry.viewmodel.TelemetryViewModel

/**
 * Main drive assistant screen with LiveKit integration
 *
 * Features:
 * - Camera preview with ROI overlays (placeholder)
 * - Telemetry HUD (G-Force, speed, etc.)
 * - LiveKit connection status
 * - Auto-connect on start
 */
@Composable
fun LiveTelemetryScreen(
    viewModel: TelemetryViewModel = hiltViewModel()
) {
    val telemetry by viewModel.telemetryFlow.collectAsState(initial = null)
    val isRunning by viewModel.isRunningFlow.collectAsState(initial = false)

    // LiveKit connection state
    val connectionState by viewModel.connectionStateFlow.collectAsState(initial = ConnectionState.Disconnected)
    val isVlaActive by viewModel.isVlaActiveFlow.collectAsState(initial = false)

    // Auto-start VLA session on first composition
    LaunchedEffect(Unit) {
        if (!isVlaActive) {
            viewModel.startVlaSession()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Placeholder for camera preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷 Camera Preview",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera preview placeholder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        // Telemetry HUD overlay
        if (isRunning && telemetry != null) {
            TelemetryHud(
                telemetry = telemetry!!,
                modifier = Modifier.fillMaxSize()
            )
        }

        // LiveKit Connection Status (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            ConnectionStatusBar(
                connectionState = connectionState,
                errorMessage = null
            )
        }

        // VLA Session indicator (bottom-left)
        if (isVlaActive && connectionState is ConnectionState.Connected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(
                        Color(0xFF00E676).copy(alpha = 0.2f),
                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp, 8.dp)
            ) {
                Text(
                    text = "🤖 AI Co-driver Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
