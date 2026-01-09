package com.pacenote.vla

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pacenote.vla.core.ui.theme.PaceNoteVLATheme
import com.pacenote.vla.ui.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * PaceNote VLA v2.0 - Pure Edge Edition
 *
 * Zero cloud dependencies, zero alignment issues.
 * Pure on-device AI with Android 16 (API 36) support.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PaceNoteVLATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )

                    // Direct to dashboard in v2.0 - no auth required for pure edge
                    DashboardScreen()
                }
            }
        }
    }
}
