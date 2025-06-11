package com.example.yatratrack.helper

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

class WorkManagerHelper(private val context: Context) {

    companion object {
        const val LOCATION_WORK_NAME = "location_tracking_work"
        const val IMMEDIATE_LOCATION_WORK_NAME = "immediate_location_work"
        const val TAG = "WorkManagerHelper"
    }

    fun startLocationTracking() {
        try {
            // More lenient constraints to prevent background execution issues
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false) // Don't require battery not low
                .setRequiresDeviceIdle(false)    // Don't require device idle
                .setRequiresCharging(false)      // Don't require charging
                .setRequiresStorageNotLow(false) // Don't require storage not low
                .build()

            val locationWorkRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
                15, TimeUnit.MINUTES,     // Repeat interval
                5, TimeUnit.MINUTES       // Flex interval - allows execution within 5 minutes of scheduled time
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,  // Use exponential backoff instead of linear
                    15, TimeUnit.MINUTES        // Initial backoff delay
                )
                .addTag("location_tracking")
                .addTag("persistent_work")
                .build()

            Log.d(TAG, "Enqueuing periodic location tracking work")

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                LOCATION_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already running
                locationWorkRequest
            )

            Log.d(TAG, "Location tracking work enqueued successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
        }
    }

    fun fetchLocationImmediately() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build()

            val immediateLocationRequest = OneTimeWorkRequestBuilder<LocationTrackingWorker>()
                .setConstraints(constraints)
                .addTag("immediate_location")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.SECONDS
                )
                .build()

            Log.d(TAG, "Enqueuing immediate location fetch")

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_LOCATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateLocationRequest
            )

            Log.d(TAG, "Immediate location work enqueued successfully")

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

    fun stopLocationTracking() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(LOCATION_WORK_NAME)
            Log.d(TAG, "Location tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        }
    }

    fun stopImmediateLocationFetch() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_LOCATION_WORK_NAME)
            Log.d(TAG, "Immediate location fetch stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping immediate location fetch", e)
        }
    }

    fun getWorkStatus(): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(LOCATION_WORK_NAME)
    }

    fun getImmediateWorkStatus(): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(IMMEDIATE_LOCATION_WORK_NAME)
    }

    fun getAllLocationWorkStatus(): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosByTagLiveData("location_tracking")
    }

    fun cancelAllLocationWork() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(LOCATION_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_LOCATION_WORK_NAME)
            WorkManager.getInstance(context).cancelAllWorkByTag("location_tracking")
            WorkManager.getInstance(context).cancelAllWorkByTag("immediate_location")
            Log.d(TAG, "All location work cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling all location work", e)
        }
    }

    fun isLocationTrackingActive(): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(LOCATION_WORK_NAME)
                .get()

            workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking work status", e)
            false
        }
    }

    fun getLastSuccessfulFetch(): Long {
        val sharedPrefs = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getLong("last_successful_fetch", 0L)
    }
}