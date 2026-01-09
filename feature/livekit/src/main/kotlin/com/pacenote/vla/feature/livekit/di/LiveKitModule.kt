package com.pacenote.vla.feature.livekit.di

import android.content.Context
import com.pacenote.vla.feature.livekit.client.LiveKitClient
import com.pacenote.vla.feature.livekit.client.LiveKitConfig
import com.pacenote.vla.feature.livekit.client.LiveKitTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * LiveKit DI Module with production credentials
 */
@Module
@InstallIn(SingletonComponent::class)
object LiveKitModule {

    // LiveKit Cloud Production Credentials
    private const val LIVEKIT_URL = "wss://pacenote-vla-0h5nqlua.livekit.cloud"
    private const val API_KEY = "APIaJwkdYot8oe5"
    private const val API_SECRET = "wAr6LVXeIMAGmDieyjP4IbGjLFgDjreRaLors6fX1UTA"

    @Provides
    @Singleton
    fun provideLiveKitConfig(): LiveKitConfig {
        return LiveKitConfig(
            serverUrl = LIVEKIT_URL,
            apiKey = API_KEY,
            apiSecret = API_SECRET,
            roomNamePrefix = "pacenote-vla-session-"
        )
    }

    @Provides
    @Singleton
    fun provideLiveKitTokenManager(config: LiveKitConfig): LiveKitTokenManager {
        return LiveKitTokenManager(config)
    }

    @Provides
    @Singleton
    fun provideLiveKitClient(
        @ApplicationContext context: Context,
        config: LiveKitConfig,
        tokenManager: LiveKitTokenManager
    ): LiveKitClient {
        return LiveKitClient(context, config, tokenManager)
    }
}
