package com.pacenote.vla.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacenote.vla.core.domain.model.UserProfile
import com.pacenote.vla.feature.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Authentication ViewModel
 * Handles sign-in, sign-out, and user session management
 *
 * NOTE: Uses mock environment without Firebase/Google Play Services
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authStateFlow: StateFlow<AuthState> = _authState.asStateFlow()

    val isAuthenticated: Flow<Boolean>
        get() = authRepository.isAuthenticated

    val currentUserFlow = authRepository.currentUserFlow

    init {
        viewModelScope.launch {
            // Check if user is already signed in
            val user = authRepository.getUserProfile()
            if (user != null) {
                _authState.value = AuthState.Authenticated(user)
            }
        }
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.signInWithEmail(email, password)

            result.onSuccess { _ ->
                val profile = authRepository.getUserProfile()
                profile?.let {
                    _authState.value = AuthState.Authenticated(profile)
                }
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    /**
     * Create account with email and password
     */
    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            val result = authRepository.createAccount(email, password)

            result.onSuccess { _ ->
                val profile = authRepository.getUserProfile()
                profile?.let {
                    _authState.value = AuthState.Authenticated(profile)
                }
            }.onFailure { e ->
                _authState.value = AuthState.Error(e.message ?: "Account creation failed")
            }
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Idle
            Timber.i("User signed out")
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onFailure { e ->
                    _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
                }
        }
    }
}

/**
 * Authentication states
 */
sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}
