package com.pacenote.vla.core.aibrain.di

import com.pacenote.vla.core.aibrain.VlaBrain
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiBrainModule {

    @Provides
    @Singleton
    fun provideVlaBrain(): VlaBrain {
        return VlaBrain()
    }
}
