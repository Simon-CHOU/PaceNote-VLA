package com.pacenote.vla.feature.auth.service

import com.pacenote.vla.core.domain.model.SubscriptionTier
import com.pacenote.vla.core.domain.model.User
import com.pacenote.vla.core.domain.model.UserProfile
import com.pacenote.vla.core.domain.service.AuthService
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
 * Offline authentication service for local testing
 * Returns hardcoded test user, independent of Firebase SDK
 */
@Singleton
class OfflineAuthService @Inject constructor() : AuthService {

    private val hardcodedUser = User(
        id = "offline_user_12345",
        email = "test@pacenote.local",
        displayName = "Test Driver",
        photoUrl = null,
        subscriptionTier = SubscriptionTier.PRO, // Always Pro for testing
        isEmailVerified = true
    )

    private val _currentUser = MutableStateFlow<User?>(hardcodedUser)

    override val currentUser: Flow<User?> = _currentUser.asStateFlow()

    override val isAuthenticated: Flow<Boolean> = _currentUser.map { it != null }

    init {
        Timber.i("OfflineAuthService initialized - user: ${hardcodedUser.email}")
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        Timber.d("Offline sign-in with email: $email")
        _currentUser.value = hardcodedUser
        return Result.success(hardcodedUser)
    }

    override suspend fun createAccount(email: String, password: String): Result<User> {
        Timber.d("Offline create account: $email")
        _currentUser.value = hardcodedUser
        return Result.success(hardcodedUser)
    }

    override suspend fun signOut(): Result<Unit> {
        Timber.d("Offline sign-out")
        _currentUser.value = null
        return Result.success(Unit)
    }

    override suspend fun reloadUser(): Result<Unit> {
        Timber.d("Offline user reload")
        _currentUser.value = hardcodedUser
        return Result.success(Unit)
    }

    override fun getUserProfile(): UserProfile? {
        val user = _currentUser.value ?: return null

        return UserProfile(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            subscriptionTier = user.subscriptionTier,
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
}
