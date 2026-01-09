package com.pacenote.vla.feature.vision.di

import com.pacenote.vla.feature.vision.VisionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VisionModule {

    @Provides
    @Singleton
    fun provideVisionManager(
        vlaBrain: com.pacenote.vla.core.aibrain.VlaBrain
    ): VisionManager {
        return VisionManager(vlaBrain)
    }
}
