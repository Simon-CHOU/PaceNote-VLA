package com.pacenote.vla.feature.monetization.di

import com.pacenote.vla.core.domain.service.PurchaseService
import com.pacenote.vla.feature.monetization.service.DebugPurchaseService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module that provides DebugPurchaseService
 * Use this instead of BillingModule for local testing without Play Billing
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugPurchaseModule {

    @Binds
    @Singleton
    abstract fun bindPurchaseService(
        debugPurchaseService: DebugPurchaseService
    ): PurchaseService
}
