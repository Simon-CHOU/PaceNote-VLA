package com.pacenote.vla.di

import com.pacenote.vla.feature.auth.di.OfflineAuthModule
import com.pacenote.vla.feature.monetization.di.DebugPurchaseModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Mock Dependencies Module
 *
 * This module enables mock implementations for local testing.
 * All dependencies in MockAppModule will replace production implementations.
 *
 * To switch between mock and production:
 * 1. For MOCK: Keep this module enabled, comment out Firebase/Play Billing plugins
 * 2. For PRODUCTION: Disable this module, enable Firebase/Play Billing plugins
 */
@Module(
    includes = [
        OfflineAuthModule::class,
        DebugPurchaseModule::class
    ]
)
@InstallIn(SingletonComponent::class)
object MockDependenciesModule
