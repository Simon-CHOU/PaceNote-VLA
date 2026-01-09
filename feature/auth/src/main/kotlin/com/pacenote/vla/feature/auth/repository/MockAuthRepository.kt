package com.pacenote.vla.feature.auth.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.pacenote.vla.core.domain.model.SubscriptionTier
import com.pacenote.vla.core.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock authentication repository for local testing
 * Bypasses Firebase authentication for development
 * Uses composition instead of inheritance for flexibility
 */
@Singleton
class MockAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Mock user data
    private val mockUid = "mock_user_12345"
    private val mockEmail = "test@pacenote.local"
    private val mockDisplayName = "Test Driver"

    private val _currentUser = MutableStateFlow<String?>(mockUid)
    val currentUserFlow: StateFlow<String?> = _currentUser.asStateFlow()

    val isAuthenticated: Flow<Boolean>
        get() = _currentUser.map { it != null }

    init {
        Timber.i("MockAuthRepository initialized - user auto-authenticated")
    }

    fun getGoogleSignInIntent() = null // No intent needed for mock

    suspend fun signInWithGoogle(account: GoogleSignInAccount?): Result<String> {
        return Result.success(mockUid)
    }

    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        Timber.d("Mock sign-in with email: $email")
        _currentUser.value = mockUid
        return Result.success(mockUid)
    }

    suspend fun createAccount(email: String, password: String): Result<String> {
        Timber.d("Mock create account: $email")
        _currentUser.value = mockUid
        return Result.success(mockUid)
    }

    suspend fun signOut(): Result<Unit> {
        Timber.d("Mock sign-out")
        _currentUser.value = null
        return Result.success(Unit)
    }

    fun getUserProfile(): UserProfile? {
        val uid = _currentUser.value ?: return null

        return UserProfile(
            userId = uid,
            email = mockEmail,
            displayName = mockDisplayName,
            photoUrl = null,
            subscriptionTier = SubscriptionTier.PRO, // Always Pro for testing
            trialStartDate = System.currentTimeMillis(),
            subscriptionExpiry = null,
            preferredLanguage = "en",
            isLhd = true,
            voiceEnabled = true,
            hapticEnabled = true,
            totalSessions = 0,
            totalDistanceMeters = 0L,
            lastSessionDate = null
        )
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        Timber.d("Mock password reset for: $email")
        return Result.success(Unit)
    }

    suspend fun deleteAccount(): Result<Unit> {
        Timber.d("Mock account deletion")
        _currentUser.value = null
        return Result.success(Unit)
    }

    suspend fun reloadUser(): Result<Unit> {
        Timber.d("Mock user reload")
        _currentUser.value = mockUid
        return Result.success(Unit)
    }

    fun getUserId(): String? = _currentUser.value
    fun getEmail(): String? = if (_currentUser.value != null) mockEmail else null
    fun getDisplayName(): String? = if (_currentUser.value != null) mockDisplayName else null
    fun getContext(): Context = context
}
