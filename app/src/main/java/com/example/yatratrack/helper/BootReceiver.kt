package com.example.yatratrack.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot completed or package updated, checking if location tracking should restart")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {

                // Check if location tracking was previously enabled
                val prefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
                val wasTrackingEnabled = prefs.getBoolean("location_tracking_enabled", false)

                if (wasTrackingEnabled) {
                    Log.d(TAG, "Location tracking was enabled, restarting service")

                    try {
                        // Restart the location tracking service
                        LocationTrackingService.startLocationTracking(context)
                        Log.d(TAG, "Location tracking service restarted successfully after boot")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restart location tracking service after boot", e)
                    }
                } else {
                    Log.d(TAG, "Location tracking was not enabled, no action needed")
                }
            }
        }
    }
}