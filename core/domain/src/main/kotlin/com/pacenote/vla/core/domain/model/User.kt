package com.pacenote.vla.core.domain.model

/**
 * Simple User data class for offline testing
 * Independent of Firebase SDK
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE_TRIAL,
    val isEmailVerified: Boolean = true
) {
    val isPro: Boolean
        get() = subscriptionTier == SubscriptionTier.PRO ||
                (subscriptionTier == SubscriptionTier.FREE_TRIAL &&
                 (trialStartDate != null && trialStartDate!! > System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))

    val trialStartDate: Long? = null
}
