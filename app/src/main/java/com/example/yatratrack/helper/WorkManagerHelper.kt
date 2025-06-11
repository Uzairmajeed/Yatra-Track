package com.example.yatratrack.helper

import android.content.Context
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
    }

    fun startLocationTracking() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresDeviceIdle(false)
            .setRequiresCharging(false)
            .build()

        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.Companion.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("location_tracking")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LOCATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            locationWorkRequest
        )
    }

    fun fetchLocationImmediately() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val immediateLocationRequest = OneTimeWorkRequestBuilder<LocationTrackingWorker>()
            .setConstraints(constraints)
            .addTag("immediate_location")
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.Companion.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_LOCATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateLocationRequest
        )
    }

    fun stopLocationTracking() {
        WorkManager.getInstance(context).cancelUniqueWork(LOCATION_WORK_NAME)
    }

    fun stopImmediateLocationFetch() {
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_LOCATION_WORK_NAME)
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
        WorkManager.getInstance(context).cancelUniqueWork(LOCATION_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_LOCATION_WORK_NAME)
    }
}