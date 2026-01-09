package com.pacenote.vla

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class PaceNoteApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Drive Assistant Service Channel
            val driveAssistantChannel = NotificationChannel(
                CHANNEL_DRIVE_ASSISTANT,
                "Drive Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time driving assistance and monitoring"
            }

            // Critical Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical safety notifications"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(driveAssistantChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    companion object {
        const val CHANNEL_DRIVE_ASSISTANT = "drive_assistant_service"
        const val CHANNEL_ALERTS = "safety_alerts"
    }
}
