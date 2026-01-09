package com.pacenote.vla.feature.auth.di

import com.pacenote.vla.core.domain.service.AuthService
import com.pacenote.vla.feature.auth.service.OfflineAuthService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module that provides OfflineAuthService
 * Use this instead of AuthModule for local testing without Firebase
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineAuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthService(
        offlineAuthService: OfflineAuthService
    ): AuthService
}
