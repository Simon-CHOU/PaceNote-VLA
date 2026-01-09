package com.pacenote.vla.feature.telemetry.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacenote.vla.core.domain.model.GForceVector
import com.pacenote.vla.core.domain.model.TelemetryData

// Define HUD colors locally (same as in GForceMeter.kt)
private val HudBackground = Color(0xFF1A1A1A)
private val HudForeground = Color(0xFFE0E0E0)

/**
 * Complete telemetry HUD with all WRC-style elements
 */
@Composable
fun TelemetryHud(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Top bar - GPS and status
        GpsBar(
            latitude = telemetry.latitude,
            longitude = telemetry.longitude,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp)
        )

        // Left side - G-Force meter
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
        ) {
            GForceFrictionCircle(
                gForce = GForceVector(
                    longitudinal = telemetry.longitudinalG,
                    lateral = telemetry.lateralG
                )
            )
        }

        // Bottom - Speedometer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            DigitalSpeedometer(
                speedMps = telemetry.speedMps
            )
        }

        // Right side - Additional telemetry
        AdditionalTelemetry(
            telemetry = telemetry,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun GpsBar(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(HudBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = String.format(
                "LAT: %.6f | LON: %.6f",
                latitude,
                longitude
            ),
            color = HudForeground,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AdditionalTelemetry(
    telemetry: TelemetryData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TelemetryLabel(
            label = "YAW RATE",
            value = String.format("%.1f°/s", telemetry.yawRate)
        )
        TelemetryLabel(
            label = "ACC X",
            value = String.format("%.2f m/s²", telemetry.accelerationX)
        )
        TelemetryLabel(
            label = "ACC Y",
            value = String.format("%.2f m/s²", telemetry.accelerationY)
        )
        TelemetryLabel(
            label = "ACC Z",
            value = String.format("%.2f m/s²", telemetry.accelerationZ)
        )
        TelemetryLabel(
            label = "GYRO X",
            value = String.format("%.3f rad/s", telemetry.gyroscopeX)
        )
        TelemetryLabel(
            label = "GYRO Y",
            value = String.format("%.3f rad/s", telemetry.gyroscopeY)
        )
        TelemetryLabel(
            label = "GYRO Z",
            value = String.format("%.3f rad/s", telemetry.gyroscopeZ)
        )
    }
}

@Composable
private fun TelemetryLabel(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .background(HudBackground, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = HudForeground,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }
}
