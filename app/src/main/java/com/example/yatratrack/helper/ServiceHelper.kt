package com.example.yatratrack.helper

import android.content.Context
import android.util.Log

class ServiceHelper(private val context: Context) {

    companion object {
        const val TAG = "ServiceHelper"
    }

    fun startLocationTracking() {
        try {
            Log.d(TAG, "Starting location tracking with service")

            // Mark tracking as enabled
            setTrackingEnabled(true)

            // Start the location tracking service
            LocationTrackingService.startLocationTracking(context)

            Log.d(TAG, "Location tracking started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
        }
    }

    fun stopLocationTracking() {
        try {
            Log.d(TAG, "Stopping location tracking")

            // Mark tracking as disabled
            setTrackingEnabled(false)

            // Stop the location tracking service
            LocationTrackingService.stopLocationTracking(context)

            Log.d(TAG, "Location tracking stopped successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        }
    }

    fun fetchLocationImmediately() {
        try {
            Log.d(TAG, "Fetching location immediately")

            // Trigger immediate location fetch
            LocationTrackingService.fetchLocationNow(context)

            Log.d(TAG, "Immediate location fetch triggered")

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching immediate location", e)
        }
    }

    fun restartLocationTracking() {
        try {
            Log.d(TAG, "Restarting location tracking")
            stopLocationTracking()
            // Small delay to ensure cleanup
            Thread.sleep(1000)
            startLocationTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting location tracking", e)
        }
    }

    fun isLocationTrackingActive(): Boolean {
        return try {
            val alarmHelper = AlarmManagerHelper(context)
            alarmHelper.isLocationTrackingActive()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking tracking status", e)
            false
        }
    }

    fun getLastSuccessfulFetch(): Long {
        val sharedPrefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getLong("last_successful_fetch", 0L)
    }

    private fun setTrackingEnabled(enabled: Boolean) {
        val sharedPrefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("location_tracking_enabled", enabled).apply()
    }
}