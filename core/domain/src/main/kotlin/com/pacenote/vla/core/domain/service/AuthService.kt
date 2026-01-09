package com.pacenote.vla.core.domain.service

import com.pacenote.vla.core.domain.model.User
import com.pacenote.vla.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Simple authentication service interface
 * Independent of Firebase SDK
 */
interface AuthService {
    val currentUser: Flow<User?>
    val isAuthenticated: Flow<Boolean>

    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun createAccount(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun reloadUser(): Result<Unit>
    fun getUserProfile(): UserProfile?
}
