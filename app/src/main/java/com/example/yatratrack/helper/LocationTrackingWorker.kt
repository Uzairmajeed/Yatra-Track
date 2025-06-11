package com.example.yatratrack.helper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.yatratrack.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val accuracy: Float? = null
)

class LocationTrackingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val gson = Gson()

    companion object {
        const val TAG = "LocationTrackingWorker"
        const val LOCATIONS_KEY = "tracked_locations"
        const val PREFS_NAME = "yatra_prefs"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "LocationTrackingWorker started")

            // Create foreground notification for long-running task
            setForeground(createForegroundInfo())

            // Check if location permissions are granted
            if (!hasLocationPermissions()) {
                Log.w(TAG, "Location permissions not granted")
                return Result.failure()
            }

            // Get current location
            val location = getCurrentLocation()

            if (location != null) {
                // Save location to SharedPreferences
                saveLocationToPrefs(location)
                Log.d(TAG, "Location saved: ${location.latitude}, ${location.longitude}")
                Result.success()
            } else {
                Log.w(TAG, "Failed to get location")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationTrackingWorker", e)
            Result.failure()
        }
    }

    // FIXED: Added proper foreground service type for Android 12+
    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Yatra Track")
            .setContentText("Tracking your location for safety")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ (API 29+), specify the foreground service type
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            // For older versions
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking for safety"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for API < 29
        }

        Log.d(TAG, "Permissions - Fine: $fineLocation, Coarse: $coarseLocation, Background: $backgroundLocation")

        return (fineLocation || coarseLocation) && backgroundLocation
    }

    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resumeWithException(SecurityException("Location permissions not granted"))
                return@suspendCancellableCoroutine
            }

            val cancellationTokenSource = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            // Use PRIORITY_BALANCED_POWER_ACCURACY for background location
            val priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            } else {
                Priority.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                Log.d(TAG, "Location obtained: $location")
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get location", exception)
                continuation.resumeWithException(exception)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            continuation.resumeWithException(e)
        }
    }

    private fun saveLocationToPrefs(location: Location) {
        val sharedPrefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Get existing locations
        val existingLocationsJson = sharedPrefs.getString(LOCATIONS_KEY, "[]")
        val existingLocations: MutableList<LocationData> = try {
            val type = object : TypeToken<List<LocationData>>() {}.type
            gson.fromJson(existingLocationsJson, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        // Create new location data
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newLocation = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = dateFormat.format(Date()),
            accuracy = location.accuracy
        )

        // Add new location
        existingLocations.add(newLocation)

        // Keep only last 100 locations to prevent storage bloat
        if (existingLocations.size > 100) {
            existingLocations.removeAt(0)
        }

        // Save back to SharedPreferences
        val updatedJson = gson.toJson(existingLocations)
        sharedPrefs.edit().putString(LOCATIONS_KEY, updatedJson).apply()

        Log.d(TAG, "Total locations saved: ${existingLocations.size}")
    }
}