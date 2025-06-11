package com.example.yatratrack.helper

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager

class YatraTrackApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        Log.d("YatraTrackApp", "Application onCreate called")

        try {
            // Initialize WorkManager with custom configuration
            WorkManager.initialize(
                this,
                getWorkManagerConfiguration()
            )
            Log.d("YatraTrackApp", "WorkManager initialized successfully")
        } catch (e: IllegalStateException) {
            Log.e("YatraTrackApp", "WorkManager initialization failed", e)
            // WorkManager is already initialized, which is fine
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    }
}