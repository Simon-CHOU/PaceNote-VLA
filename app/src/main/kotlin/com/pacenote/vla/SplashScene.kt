package com.pacenote.vla

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacenote.vla.feature.auth.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SplashScene(
    onAuthComplete: (Boolean) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(isAuthenticated) {
        delay(800)
        onAuthComplete(isAuthenticated)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo placeholder
            Text(
                text = "PaceNote",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B00),
                fontSize = 48.sp
            )
            Text(
                text = "VLA",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.alpha(0.7f),
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your AI Co-driver",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
