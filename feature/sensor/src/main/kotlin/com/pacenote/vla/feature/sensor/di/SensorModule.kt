package com.pacenote.vla.feature.sensor.di

import android.content.Context
import com.pacenote.vla.feature.sensor.manager.SensorFusionManager
import com.pacenote.vla.feature.sensor.manager.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideSensorFusionManager(
        @ApplicationContext context: Context
    ): SensorFusionManager {
        return SensorFusionManager(context)
    }

    @Provides
    @Singleton
    fun provideLocationManager(
        @ApplicationContext context: Context
    ): LocationManager {
        return LocationManager(context)
    }
}
