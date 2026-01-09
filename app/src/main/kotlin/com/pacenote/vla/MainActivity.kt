package com.pacenote.vla

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.pacenote.vla.core.ui.theme.PaceNoteVLATheme
import com.pacenote.vla.feature.auth.viewmodel.AuthViewModel
import com.pacenote.vla.feature.auth.ui.AuthScreen
import com.pacenote.vla.feature.auth.navigation.authGraph
import com.pacenote.vla.feature.telemetry.navigation.telemetryGraph
import com.pacenote.vla.feature.monetization.navigation.monetizationGraph
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

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
                    val navController = rememberNavController()
                    val permissionsState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScene(
                                onAuthComplete = { isAuthenticated ->
                                    navController.navigate(
                                        if (isAuthenticated) "drive_assistant" else "auth"
                                    ) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        authGraph(navController)
                        telemetryGraph()
                        monetizationGraph(navController)
                    }
                }
            }
        }
    }
}
