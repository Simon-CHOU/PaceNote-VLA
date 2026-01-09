package com.pacenote.vla.feature.telemetry.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pacenote.vla.feature.telemetry.ui.LiveTelemetryScreen

const val TELEMETRY_ROUTE = "drive_assistant"

fun NavGraphBuilder.telemetryGraph() {
    composable(TELEMETRY_ROUTE) {
        LiveTelemetryScreen()
    }
}
