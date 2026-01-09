package com.pacenote.vla.feature.monetization.service

import android.app.Activity
import com.pacenote.vla.core.domain.service.PurchaseService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug purchase service for local testing
 * Returns hardcoded isPro = true, independent of Play Billing SDK
 */
@Singleton
class DebugPurchaseService @Inject constructor() : PurchaseService {

    private val _isPro = MutableStateFlow(true) // Always Pro for testing
    override val isPro: Flow<Boolean> = _isPro.asStateFlow()

    init {
        Timber.i("DebugPurchaseService initialized - Pro status enabled")
    }

    override suspend fun initialize(): Result<Unit> {
        Timber.d("Debug purchase initialize")
        return Result.success(Unit)
    }

    override suspend fun launchPurchaseFlow(activity: Activity): Result<Unit> {
        Timber.d("Debug launch purchase flow")
        // Simulate successful purchase
        _isPro.value = true
        return Result.success(Unit)
    }

    override fun toggleProStatus() {
        _isPro.value = !_isPro.value
        Timber.i("Pro status toggled to: ${_isPro.value}")
    }

    override fun disconnect() {
        Timber.d("Debug purchase disconnect")
    }
}
