package com.pacenote.vla.core.domain.model

import kotlinx.serialization.Serializable

/**
 * User profile and subscription status
 */
@Serializable
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,

    // Subscription
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE_TRIAL,
    val trialStartDate: Long? = null,
    val subscriptionExpiry: Long? = null,

    // Preferences
    val preferredLanguage: String = "en",
    val isLhd: Boolean = true,
    val voiceEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,

    // Usage tracking
    val totalSessions: Int = 0,
    val totalDistanceMeters: Long = 0L,
    val lastSessionDate: Long? = null
) {
    val isPro: Boolean
        get() = subscriptionTier == SubscriptionTier.PRO &&
                (subscriptionExpiry == null || subscriptionExpiry > System.currentTimeMillis())
}

@Serializable
enum class SubscriptionTier {
    FREE_TRIAL,      // 7-day trial
    FREE_AD_SUPPORTED, // After trial, with ads
    PRO,             // Paid subscription
    LEGACY           // Grandfathered users
}

/**
 * Session data for logging
 */
@Serializable
data class DriveSession(
    val sessionId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val distanceMeters: Long = 0L,
    val maxSpeedMps: Float = 0f,
    val maxGForce: Float = 0f,
    val aiCommentaryCount: Int = 0,
    val reflexAlertCount: Int = 0,
    val recordingUrl: String? = null
)
