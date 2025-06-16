package com.example.yatratrack.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmManagerHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val TAG = "AlarmManagerHelper"
        const val LOCATION_FETCH_REQUEST_CODE = 1001
        const val INTERVAL_MILLIS = 15 * 60 * 1000L // 15 minutes
    }

    fun startPeriodicLocationFetch() {
        try {
            Log.d(TAG, "Starting periodic location fetch")

            // Request to ignore battery optimization if not already done
            requestIgnoreBatteryOptimization()

            val intent = Intent(context, LocationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                LOCATION_FETCH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + INTERVAL_MILLIS

            // Use setExactAndAllowWhileIdle for better reliability on all Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For API 23+, use setExactAndAllowWhileIdle to work in Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Using setExactAndAllowWhileIdle for API ${Build.VERSION.SDK_INT}")
            } else {
                // For older versions, use setExact
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Using setExact for API ${Build.VERSION.SDK_INT}")
            }

            // Save alarm state
            saveAlarmState(true)

            Log.d(TAG, "Periodic location fetch scheduled successfully for ${java.util.Date(triggerTime)}")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting periodic location fetch", e)
        }
    }

    fun stopPeriodicLocationFetch() {
        try {
            Log.d(TAG, "Stopping periodic location fetch")

            val intent = Intent(context, LocationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                LOCATION_FETCH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            // Save alarm state
            saveAlarmState(false)

            Log.d(TAG, "Periodic location fetch stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping periodic location fetch", e)
        }
    }

    fun rescheduleNextAlarm() {
        if (isAlarmActive()) {
            try {
                Log.d(TAG, "Rescheduling next alarm")

                val intent = Intent(context, LocationAlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    LOCATION_FETCH_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val nextTriggerTime = System.currentTimeMillis() + INTERVAL_MILLIS

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                }

                Log.d(TAG, "Next alarm scheduled for: ${java.util.Date(nextTriggerTime)}")

            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarm", e)
            }
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                    Log.d(TAG, "App is not exempt from battery optimization")

                    // Note: We're not automatically requesting exemption here as it requires user interaction
                    // The app should handle this in the UI layer
                } else {
                    Log.d(TAG, "App is already exempt from battery optimization")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status", e)
        }
    }

    fun isAppExemptFromBatteryOptimization(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true // Not applicable for older versions
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status", e)
            false
        }
    }

    private fun saveAlarmState(active: Boolean) {
        val prefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("alarm_active", active).apply()
        // Also save the timestamp when alarm was set
        if (active) {
            prefs.edit().putLong("alarm_set_time", System.currentTimeMillis()).apply()
        }
    }

    private fun isAlarmActive(): Boolean {
        val prefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("alarm_active", false)
    }

    fun isLocationTrackingActive(): Boolean {
        return isAlarmActive()
    }

    fun getLastAlarmSetTime(): Long {
        val prefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("alarm_set_time", 0L)
    }
}