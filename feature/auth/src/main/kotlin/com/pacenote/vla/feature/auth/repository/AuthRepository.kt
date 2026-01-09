package com.pacenote.vla.feature.auth.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.pacenote.vla.core.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication repository
 * Supports: Email/Password, Google Sign-In
 */
@Singleton
open class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    open val currentUserFlow: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    open val isAuthenticated: Flow<Boolean>
        get() = currentUserFlow.map { it != null }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("mock-web-client-id") // Mock value for testing
            .requestEmail()
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    init {
        // Listen for auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            _currentUser.value = auth.currentUser
            Timber.d("Auth state changed: ${auth.currentUser?.uid}")
        }
    }

    /**
     * Get Google Sign-In intent
     */
    open fun getGoogleSignInIntent() = googleSignInClient.signInIntent

    /**
     * Handle Google Sign-In result
     */
    open suspend fun signInWithGoogle(account: GoogleSignInAccount?): Result<FirebaseUser> {
        return try {
            if (account == null) {
                return Result.failure(IllegalArgumentException("Google sign-in account is null"))
            }

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            if (user != null) {
                Timber.i("Google sign-in successful: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Sign-in failed: user is null"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Google sign-in failed")
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password
     */
    open suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Timber.i("Email sign-in successful: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Sign-in failed: user is null"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Email sign-in failed")
            Result.failure(e)
        }
    }

    /**
     * Create account with email and password
     */
    open suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                Timber.i("Account created: ${user.uid}")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Account creation failed: user is null"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Account creation failed")
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    open suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            googleSignInClient.signOut().await()
            Timber.i("User signed out")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign-out failed")
            Result.failure(e)
        }
    }

    /**
     * Get current user profile
     */
    open fun getUserProfile(): UserProfile? {
        val user = firebaseAuth.currentUser ?: return null

        return UserProfile(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
            subscriptionTier = com.pacenote.vla.core.domain.model.SubscriptionTier.FREE_TRIAL,
            trialStartDate = System.currentTimeMillis(),
            subscriptionExpiry = null
        )
    }

    /**
     * Send password reset email
     */
    open suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Timber.i("Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send password reset email")
            Result.failure(e)
        }
    }

    /**
     * Delete current user account
     */
    open suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(IllegalStateException("No user signed in"))

            user.delete().await()
            Timber.i("Account deleted: ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Account deletion failed")
            Result.failure(e)
        }
    }

    /**
     * Reload user data
     */
    open suspend fun reloadUser(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(IllegalStateException("No user signed in"))

            user.reload().await()
            _currentUser.value = firebaseAuth.currentUser
            Timber.d("User data reloaded")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to reload user")
            Result.failure(e)
        }
    }
}
