package com.pacenote.vla.feature.monetization.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pacenote.vla.core.domain.service.PurchaseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Monetization ViewModel
 * Handles subscriptions and ad management
 *
 * NOTE: Uses PurchaseService interface (DebugPurchaseService for testing)
 */
@HiltViewModel
class MonetizationViewModel @Inject constructor(
    private val purchaseService: PurchaseService
) : ViewModel() {

    val isProFlow: StateFlow<Boolean> = purchaseService.isPro
        .let { flow ->
            // Convert Flow to StateFlow with initial value
            kotlinx.coroutines.flow.MutableStateFlow(false).apply {
                viewModelScope.launch {
                    flow.collect { value = it }
                }
            }
        }

    init {
        viewModelScope.launch {
            purchaseService.initialize()
                .onFailure { e ->
                    Timber.e(e, "Failed to initialize purchase service")
                }
        }
    }

    /**
     * Purchase subscription
     */
    fun purchaseSubscription(activity: android.app.Activity) {
        viewModelScope.launch {
            purchaseService.launchPurchaseFlow(activity)
                .onFailure { e ->
                    Timber.e(e, "Failed to launch billing flow")
                }
        }
    }

    /**
     * Toggle pro status (for testing/debug only)
     */
    fun toggleProStatus() {
        purchaseService.toggleProStatus()
    }

    /**
     * Check subscription status
     */
    fun checkSubscriptionStatus() {
        // Status is automatically tracked via isProFlow
        Timber.d("Checking subscription status: isPro = ${isProFlow.value}")
    }

    override fun onCleared() {
        super.onCleared()
        purchaseService.disconnect()
    }
}
