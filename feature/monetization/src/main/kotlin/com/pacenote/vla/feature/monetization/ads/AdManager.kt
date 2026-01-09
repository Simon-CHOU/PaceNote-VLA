package com.pacenote.vla.feature.monetization.ads

import android.app.Activity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdMob manager for interstitial ads (free tier)
 *
 * Strategy: Show ads between driving sessions for free tier users
 *
 * NOTE: This is a stub implementation for mock testing.
 * The full implementation requires Google AdMob SDK dependencies.
 */
@Singleton
class AdManager @Inject constructor(
) {
    private val eventChannel = Channel<AdEvent>(capacity = Channel.UNLIMITED)
    val eventFlow: Flow<AdEvent> = eventChannel.receiveAsFlow()

    /**
     * Load interstitial ad
     */
    fun loadInterstitialAd() {
        Timber.i("Interstitial ad loaded (stub)")
        eventChannel.trySend(AdEvent.AdLoaded)
    }

    /**
     * Show interstitial ad if available
     */
    fun showInterstitialAd(activity: Activity) {
        Timber.i("Interstitial ad shown (stub)")
        eventChannel.trySend(AdEvent.AdShown)
        eventChannel.trySend(AdEvent.AdDismissed)
    }

    /**
     * Check if ad is ready to show
     */
    fun isAdReady(): Boolean = true

    /**
     * Preload ad for future use
     */
    fun preloadAd() {
        loadInterstitialAd()
    }
}

/**
 * Ad events
 */
sealed class AdEvent {
    data object AdLoaded : AdEvent()
    data object AdShown : AdEvent()
    data object AdDismissed : AdEvent()
    data object AdImpression : AdEvent()
    data object NoAdAvailable : AdEvent()
    data class AdLoadFailed(val error: String?) : AdEvent()
    data class AdShowFailed(val error: String?) : AdEvent()
}
