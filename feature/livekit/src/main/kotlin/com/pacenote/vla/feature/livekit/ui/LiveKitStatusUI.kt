package com.pacenote.vla.feature.livekit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pacenote.vla.feature.livekit.client.ConnectionState

/**
 * LiveKit connection status indicator badge
 */
@Composable
fun LiveKitStatusBadge(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor, icon) = when (connectionState) {
        is ConnectionState.Connected -> Triple("LIVE", Color(0xFF00E676), Icons.Default.Wifi)
        is ConnectionState.Connecting -> Triple("CONNECTING...", Color(0xFFFFAB00), Icons.Default.Wifi)
        is ConnectionState.Disconnected -> Triple("OFFLINE", Color(0xFFFF1744), Icons.Default.Wifi)
        is ConnectionState.Error -> Triple("ERROR", Color(0xFFD50000), Icons.Default.Error)
    }

    val backgroundColor by animateColorAsState(
        targetValue = statusColor.copy(alpha = 0.2f),
        label = "statusColor"
    )

    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * LiveKit error message display
 */
@Composable
fun LiveKitErrorMessage(
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    if (errorMessage != null) {
        Row(
            modifier = modifier
                .background(Color(0xFFD50000).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorMessage,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Compact connection status bar
 */
@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LiveKitStatusBadge(connectionState)
        if (errorMessage != null) {
            LiveKitErrorMessage(errorMessage)
        }
    }
}
