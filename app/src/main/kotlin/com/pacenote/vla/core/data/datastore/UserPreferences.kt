package com.pacenote.vla.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * User preferences stored in DataStore
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val IS_LHD = booleanPreferencesKey("is_lhd")
        val LANGUAGE = stringPreferencesKey("language")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val TRIAL_START_DATE = longPreferencesKey("trial_start_date")
        val SUBSCRIPTION_TIER = stringPreferencesKey("subscription_tier")
        val SUBSCRIPTION_EXPIRY = longPreferencesKey("subscription_expiry")
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val isLhd: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LHD] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val voiceEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VOICE_ENABLED] ?: true }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTIC_ENABLED] ?: true }
    val subscriptionTier: Flow<String> = context.dataStore.data.map { it[Keys.SUBSCRIPTION_TIER] ?: "FREE_TRIAL" }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { it[Keys.USER_ID] = userId }
    }

    suspend fun setLhd(isLhd: Boolean) {
        context.dataStore.edit { it[Keys.IS_LHD] = isLhd }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setSubscriptionTier(tier: String) {
        context.dataStore.edit { it[Keys.SUBSCRIPTION_TIER] = tier }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
