package com.example.yatratrack.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
        const val LOCATION_TIMEOUT_MS = 30000L // 30 seconds timeout
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "LocationTrackingWorker started - Run attempt: ${runAttemptCount}")

            // Check if location permissions are granted
            if (!hasLocationPermissions()) {
                Log.w(TAG, "Location permissions not granted")
                return Result.failure()
            }

            // Get current location with timeout
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                getCurrentLocation()
            }

            if (location != null) {
                // Save location to SharedPreferences
                saveLocationToPrefs(location)
                Log.d(TAG, "Location saved successfully: ${location.latitude}, ${location.longitude}")

                // Update last successful fetch time
                updateLastSuccessfulFetch()

                Result.success()
            } else {
                Log.w(TAG, "Failed to get location within timeout")
                // Retry only if we haven't exceeded max attempts
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in LocationTrackingWorker", e)
            // Retry on transient errors, fail on permanent ones
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
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

            // Use balanced power accuracy for better battery life in background
            val priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY

            Log.d(TAG, "Requesting location with priority: $priority")

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                Log.d(TAG, "Location obtained: lat=${location?.latitude}, lng=${location?.longitude}, accuracy=${location?.accuracy}")
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
            Log.e(TAG, "Error parsing existing locations", e)
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

        // Keep only last 500 locations to prevent storage bloat
        if (existingLocations.size > 500) {
            existingLocations.removeAt(0)
        }

        // Save back to SharedPreferences
        val updatedJson = gson.toJson(existingLocations)
        sharedPrefs.edit().putString(LOCATIONS_KEY, updatedJson).apply()

        Log.d(TAG, "Total locations saved: ${existingLocations.size}")
    }

    private fun updateLastSuccessfulFetch() {
        val sharedPrefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_successful_fetch", System.currentTimeMillis()).apply()
    }
}