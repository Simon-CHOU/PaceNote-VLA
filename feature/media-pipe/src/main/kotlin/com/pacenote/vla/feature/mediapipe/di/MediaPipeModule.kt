package com.pacenote.vla.feature.mediapipe.di

import android.content.Context
import com.pacenote.vla.feature.mediapipe.detector.ObjectDetector
import com.pacenote.vla.feature.mediapipe.detector.DetectorConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaPipeModule {

    @Provides
    @Singleton
    fun provideDetectorConfig(): DetectorConfig {
        return DetectorConfig(
            modelPath = "object_detector.tflite", // EfficientDet Lite0 from assets
            confidenceThreshold = 0.5f,
            maxDetections = 10,
            enableGpu = true,
            enableNpu = true
        )
    }

    @Provides
    @Singleton
    fun provideObjectDetector(
        @ApplicationContext context: Context,
        config: DetectorConfig
    ): ObjectDetector {
        return ObjectDetector(context, config)
    }
}
