package com.pacenote.vla.core.domain.service

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/**
 * Simple purchase service interface
 * Independent of Play Billing SDK
 */
interface PurchaseService {
    val isPro: Flow<Boolean>

    suspend fun initialize(): Result<Unit>
    suspend fun launchPurchaseFlow(activity: Activity): Result<Unit>
    fun toggleProStatus() // For testing
    fun disconnect()
}
