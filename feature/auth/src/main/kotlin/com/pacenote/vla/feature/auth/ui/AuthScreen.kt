package com.pacenote.vla.feature.auth.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pacenote.vla.feature.auth.viewmodel.AuthState
import com.pacenote.vla.feature.auth.viewmodel.AuthViewModel

/**
 * Authentication screen with Google Sign-In
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authStateFlow.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onAuthSuccess()
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator()
            }
            else -> {
                AuthContent(
                    onGoogleSignIn = { /* Handle via activity result */ },
                    onEmailSignIn = { email, password ->
                        viewModel.signInWithEmail(email, password)
                    },
                    onCreateAccount = { email, password ->
                        viewModel.createAccount(email, password)
                    },
                    error = (authState as? AuthState.Error)?.message
                )
            }
        }
    }
}

@Composable
private fun AuthContent(
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit,
    error: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Logo
        Text(
            text = "PaceNote",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFFFF6B00) // Orange color
        )

        Text(
            text = "Your AI Co-driver",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Google Sign-In Button
        GoogleSignInButton(
            onClick = onGoogleSignIn
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "or",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email/Password form
        EmailSignInForm(
            onSignIn = onEmailSignIn,
            onCreateAccount = onCreateAccount
        )

        // Error message
        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        // Google "G" logo
        // Note: Using text instead of image for mock testing
        Text(
            text = "G",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text("Continue with Google")
    }
}

@Composable
private fun EmailSignInForm(
    onSignIn: (String, String) -> Unit,
    onCreateAccount: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCreatingAccount by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (isCreatingAccount) {
                    onCreateAccount(email, password)
                } else {
                    onSignIn(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCreatingAccount) "Create Account" else "Sign In")
        }

        TextButton(
            onClick = { isCreatingAccount = !isCreatingAccount },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isCreatingAccount) "Already have an account? Sign In"
                else "Need an account? Create one"
            )
        }
    }
}
