package com.pacenote.vla.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DriveAssistantService : Service() {

    override fun onCreate() {
        super.onCreate()
        Timber.d("DriveAssistantService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("DriveAssistantService started with action: ${intent?.action}")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("DriveAssistantService destroyed")
    }
}
