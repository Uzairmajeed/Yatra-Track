package com.example.yatratrack.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class LocationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "LocationAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received - triggering location fetch")

        try {
            // Start the location service to fetch current location
            val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_FETCH_LOCATION
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Reschedule the next alarm for API 23+ (since we use setExactAndAllowWhileIdle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmHelper = AlarmManagerHelper(context)
                alarmHelper.rescheduleNextAlarm()
            }

            Log.d(TAG, "Location fetch service started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling alarm", e)
        }
    }
}