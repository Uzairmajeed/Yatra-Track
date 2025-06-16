package com.example.yatratrack.helper

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.yatratrack.MainActivity
import com.example.yatratrack.R
import com.example.yatratrack.model.LocationRequest
import com.example.yatratrack.repository.UserRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val accuracy: Float? = null
)

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var alarmManagerHelper: AlarmManagerHelper
    private val gson = Gson()

    // HTTP client for backend communication
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("LOCATION_HTTP_CLIENT", message)
                }
            }
            level = LogLevel.BODY
        }
    }

    private val userRepository = UserRepository(httpClient)

    companion object {
        const val TAG = "LocationTrackingService"
        const val LOCATIONS_KEY = "tracked_locations"
        const val PREFS_NAME = "yatra_prefs"
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val ACTION_FETCH_LOCATION = "ACTION_FETCH_LOCATION"
        const val LOCATION_TIMEOUT_MS = 30000L

        // Foreground service constants
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"

        fun startLocationTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopLocationTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }

        fun fetchLocationNow(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_FETCH_LOCATION
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationTrackingService created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        alarmManagerHelper = AlarmManagerHelper(this)

        // Create notification channel
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                startForegroundService()
                setTrackingEnabled(true)
                alarmManagerHelper.startPeriodicLocationFetch()
                Log.d(TAG, "Location tracking started")
            }
            ACTION_STOP_TRACKING -> {
                stopLocationTracking()
            }
            ACTION_FETCH_LOCATION -> {
                // Start as foreground service for location fetch
                if (isTrackingEnabled()) {
                    startForegroundService()
                }
                serviceScope.launch {
                    fetchCurrentLocation()
                    // Stop foreground only if tracking is not enabled
                    if (!isTrackingEnabled()) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
            else -> {
                // Service restarted by system, restart tracking if it was previously active
                if (isTrackingEnabled()) {
                    startForegroundService()
                    alarmManagerHelper.startPeriodicLocationFetch()
                    Log.d(TAG, "Service restarted by system, resuming tracking")
                }
            }
        }

        return START_STICKY // Service will be restarted if killed
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Service started in foreground")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location in the background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("YatraTrack Location Tracking")
            .setContentText("Tracking your location every 15 minutes")
            .setSmallIcon(R.drawable.logo) // Make sure you have this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "LocationTrackingService destroyed")
        serviceScope.cancel()
        alarmManagerHelper.stopPeriodicLocationFetch()
        try {
            httpClient.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing HTTP client", e)
        }
        super.onDestroy()
    }

    private fun stopLocationTracking() {
        Log.d(TAG, "Stopping location tracking")
        alarmManagerHelper.stopPeriodicLocationFetch()
        setTrackingEnabled(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resumeWithException(SecurityException("Location permissions not granted"))
                return@suspendCancellableCoroutine
            }

            val cancellationTokenSource = CancellationTokenSource()

            // Try high accuracy first, fallback to balanced power
            val priority = Priority.PRIORITY_HIGH_ACCURACY

            // Handle cancellation properly
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }

            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                Log.d(TAG, "Location obtained: lat=${location?.latitude}, lng=${location?.longitude}")
                if (continuation.isActive) {
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        // If current location is null, try to get last known location as fallback
                        tryLastKnownLocation(continuation)
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get current location, trying last known", exception)
                if (continuation.isActive) {
                    // Fallback to last known location
                    tryLastKnownLocation(continuation)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception", e)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun tryLastKnownLocation(continuation: CancellableContinuation<Location?>) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (continuation.isActive) {
                        if (lastLocation != null) {
                            Log.d(TAG, "Using last known location: lat=${lastLocation.latitude}, lng=${lastLocation.longitude}")
                            continuation.resume(lastLocation)
                        } else {
                            Log.w(TAG, "No location available (current or last known)")
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        Log.e(TAG, "Failed to get last known location", exception)
                        continuation.resumeWithException(exception)
                    }
                }
            } else {
                continuation.resumeWithException(SecurityException("Location permissions not granted"))
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    // Also improve your fetchCurrentLocation function
    suspend fun fetchCurrentLocation() {
        Log.d(TAG, "Fetching current location")

        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return
        }

        try {
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                getCurrentLocation()
            }

            if (location != null) {
                // Check if location is recent enough (not older than 10 minutes)
                val locationAge = System.currentTimeMillis() - location.time
                val maxAge = 10 * 60 * 1000L // 10 minutes in milliseconds

                if (locationAge <= maxAge) {
                    // Save location locally
                    saveLocationToPrefs(location)
                    Log.d(TAG, "Location saved locally: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")

                    // Update notification with latest location
                    updateNotificationWithLocation(location)

                    // Save location to backend
                    val backendSaveSuccess = saveLocationToBackend(location)
                    if (backendSaveSuccess) {
                        Log.d(TAG, "Location also saved to backend successfully")
                    } else {
                        Log.w(TAG, "Failed to save location to backend, but local save succeeded")
                    }

                    updateLastSuccessfulFetch()
                } else {
                    Log.w(TAG, "Location is too old (${locationAge / 1000}s), skipping save")
                }
            } else {
                Log.w(TAG, "Failed to get location within timeout")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching location", e)
        }
    }

    private fun updateNotificationWithLocation(location: Location) {
        if (isTrackingEnabled()) {
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val timeString = dateFormat.format(Date())

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("YatraTrack Location Tracking")
                .setContentText("Last update: $timeString - Accuracy: ${location.accuracy.toInt()}m")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private suspend fun saveLocationToBackend(location: Location): Boolean {
        return try {
            val token = TokenManager.getToken(this)
            if (token.isNullOrEmpty()) {
                Log.w(TAG, "No JWT token found, skipping backend save")
                return false
            }

            val userId = TokenManager.getUserId(this)
            if (userId == null) {
                Log.w(TAG, "No user ID found, skipping backend save")
                return false
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())

            val locationRequest = LocationRequest(
                userId = userId,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = timestamp
            )

            userRepository.saveLocationToBackend(token, locationRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while saving location to backend", e)
            false
        }
    }

    private fun saveLocationToPrefs(location: Location) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val existingLocationsJson = sharedPrefs.getString(LOCATIONS_KEY, "[]")
        val existingLocations: MutableList<LocationData> = try {
            val type = object : TypeToken<List<LocationData>>() {}.type
            gson.fromJson(existingLocationsJson, type) ?: mutableListOf()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing existing locations", e)
            mutableListOf()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val newLocation = LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = dateFormat.format(Date()),
            accuracy = location.accuracy
        )

        existingLocations.add(newLocation)

        // Keep only last 500 locations
        if (existingLocations.size > 500) {
            existingLocations.removeAt(0)
        }

        val updatedJson = gson.toJson(existingLocations)
        sharedPrefs.edit().putString(LOCATIONS_KEY, updatedJson).apply()

        Log.d(TAG, "Total locations saved locally: ${existingLocations.size}")
    }

    private fun updateLastSuccessfulFetch() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_successful_fetch", System.currentTimeMillis()).apply()
    }

    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return (fineLocation || coarseLocation) && backgroundLocation
    }

    private fun isTrackingEnabled(): Boolean {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("location_tracking_enabled", false)
    }

    private fun setTrackingEnabled(enabled: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("location_tracking_enabled", enabled).apply()
    }
}