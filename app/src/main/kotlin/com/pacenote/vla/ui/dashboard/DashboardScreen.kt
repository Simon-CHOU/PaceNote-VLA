package com.pacenote.vla.ui.dashboard

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner as ComposeLocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.pacenote.vla.core.aibrain.VlaBrain
import com.pacenote.vla.feature.camera.manager.CameraManager
import com.pacenote.vla.feature.sensor.manager.SensorFusionManager
import com.pacenote.vla.feature.vision.VisionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = ComposeLocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val lifecycle = lifecycleOwner.lifecycle

    val uiState by viewModel.uiState.collectAsState()
    val gForce by viewModel.gForce.collectAsState()
    val aiAction by viewModel.aiAction.collectAsState(initial = null)
    val aiStatus by viewModel.aiStatus.collectAsState()
    val heartbeat by viewModel.sensorHeartbeat.collectAsState()
    val rawSensorData by viewModel.rawSensorData.collectAsState()

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val cameraGranted = cameraPermissionState.status.isGranted

    LaunchedEffect(Unit) {
        // Bind sensors to lifecycle before initializing
        viewModel.bindToLifecycle(lifecycle)
        viewModel.initialize()
    }

    LaunchedEffect(cameraGranted) {
        if (!cameraGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        if (cameraGranted && previewView != null && !uiState.isRecording) {
            Timber.tag("DashboardScreen").i("Starting camera (permissions granted)")
            viewModel.startCamera(lifecycleOwner, previewView!!)
        }
    }

    // Backup: Start camera when previewView becomes available
    LaunchedEffect(previewView) {
        if (previewView != null && cameraGranted && !uiState.isRecording) {
            Timber.tag("DashboardScreen").i("Starting camera via previewView LaunchedEffect")
            viewModel.startCamera(lifecycleOwner, previewView!!)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Preview Window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.DarkGray, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (cameraGranted) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewView = this
                            }
                        },
                        update = { view ->
                            if (previewView == null) previewView = view
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera Permission Required",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() }
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }

                // Recording indicator
                if (uiState.isRecording) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.Red, CircleShape)
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // G-Force Meter
            GForceMeter(
                longitudinalG = gForce.longitudinal,
                lateralG = gForce.lateral,
                heartbeat = heartbeat
            )

            Spacer(modifier = Modifier.height(24.dp))

            // AI Status Console
            AIStatusConsole(
                aiStatus = aiStatus,
                aiAction = aiAction,
                isConnected = uiState.isConnected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Raw Sensor Data Debug Console
            RawSensorDebugConsole(
                rawSensorData = rawSensorData,
                sensorType = "RAW SENSOR"
            )
        }
    }
}

@Composable
fun GForceMeter(
    longitudinalG: Float,
    lateralG: Float,
    heartbeat: Int
) {
    val gForceDecimal = DecimalFormat("0.00")
    val magnitude = sqrt(longitudinalG * longitudinalG + lateralG * lateralG)

    // Diagnostic: Log when composable receives values
    androidx.compose.runtime.SideEffect {
        if (magnitude > 0.01f) {
            Timber.tag("GForceMeter").d(
                "Composed with: lon=%.4f, lat=%.4f, mag=%.4f, heartbeat=%d",
                longitudinalG, lateralG, magnitude, heartbeat
            )
        }
    }

    // Color based on G-Force intensity
    val gColor = when {
        magnitude > 0.8f -> Color.Red
        magnitude > 0.5f -> Color(0xFFFFA500) // Orange
        magnitude > 0.3f -> Color.Yellow
        else -> Color(0xFF00FF00) // Green
    }

    // Heartbeat animation - changes opacity based on heartbeat count
    val heartbeatAlpha = if (heartbeat % 2 == 0) 0.3f else 1.0f

    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color(0xFF1A1A1A), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${gForceDecimal.format(magnitude)} G",
                color = gColor,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LON",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = gForceDecimal.format(longitudinalG),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LAT",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = gForceDecimal.format(lateralG),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Heartbeat indicator - center dot that flashes when sensor data arrives
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    Color.Cyan.copy(alpha = heartbeatAlpha),
                    CircleShape
                )
                .align(Alignment.Center)
        )

        // Direction indicator
        if (abs(lateralG) > 0.1f) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (lateralG > 0) Color.Cyan else Color.Magenta,
                        CircleShape
                    )
                    .align(
                        if (lateralG > 0) Alignment.CenterEnd else Alignment.CenterStart
                    )
            )
        }
    }
}

@Composable
fun AIStatusConsole(
    aiStatus: VlaBrain.AiStatus,
    aiAction: VlaBrain.VlaAction?,
    isConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A), CircleShape)
            .padding(16.dp)
    ) {
        Text(
            text = "AI Status",
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (aiStatus) {
            is VlaBrain.AiStatus.Idle -> {
                StatusRow("System", "Initializing...")
            }
            is VlaBrain.AiStatus.Initializing -> {
                StatusRow("System", "Loading AI Model...")
            }
            is VlaBrain.AiStatus.Ready -> {
                StatusRow("Model", aiStatus.modelName, Color(0xFF00FF00))
                StatusRow("Status", "Ready for Android 16", Color(0xFF00FF00))
            }
            is VlaBrain.AiStatus.Error -> {
                StatusRow("Error", aiStatus.message, Color.Red)
            }
        }

        if (aiAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val alertColor = when (aiAction.alert) {
                VlaBrain.AlertLevel.CRITICAL -> Color.Red
                VlaBrain.AlertLevel.HIGH -> Color(0xFFFFA500)
                VlaBrain.AlertLevel.MEDIUM -> Color.Yellow
                VlaBrain.AlertLevel.LOW -> Color(0xFF00FFFF)
                VlaBrain.AlertLevel.NONE -> Color.Gray
            }
            StatusRow("Alert", aiAction.alert.name, alertColor)
            StatusRow("Message", aiAction.message, Color.White)
            StatusRow("Confidence", "${(aiAction.confidence * 100).toInt()}%", Color.Cyan)
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
fun RawSensorDebugConsole(
    rawSensorData: com.pacenote.vla.feature.sensor.manager.SensorFusionManager.RawSensorData,
    sensorType: String
) {
    val debugDecimal = DecimalFormat("0.0000")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0A), CircleShape)
            .padding(16.dp)
    ) {
        Text(
            text = "RAW DEBUG ($sensorType)",
            color = Color.Yellow,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show raw event.values directly - NO processing
        StatusRow("Raw X", debugDecimal.format(rawSensorData.accelX), Color.Cyan)
        StatusRow("Raw Y", debugDecimal.format(rawSensorData.accelY), Color.Cyan)
        StatusRow("Raw Z", debugDecimal.format(rawSensorData.accelZ), Color.Cyan)

        Spacer(modifier = Modifier.height(4.dp))

        // Show magnitude for quick reference
        val magnitude = sqrt(
            rawSensorData.accelX * rawSensorData.accelX +
            rawSensorData.accelY * rawSensorData.accelY +
            rawSensorData.accelZ * rawSensorData.accelZ
        )
        StatusRow("|Raw|", debugDecimal.format(magnitude),
            when {
                magnitude > 1.0f -> Color.Red
                magnitude > 0.5f -> Color.Yellow
                magnitude > 0.1f -> Color(0xFF00FF00)
                else -> Color.Gray
            }
        )
    }
}
