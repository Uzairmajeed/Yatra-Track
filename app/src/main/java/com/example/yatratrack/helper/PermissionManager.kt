package com.example.yatratrack.helper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class PermissionManager(
    private val context: Context,
    private val serviceHelper: ServiceHelper
) {
    var hasLocationPermission by mutableStateOf(false)
        private set

    var hasBackgroundLocationPermission by mutableStateOf(false)
        private set

    var hasNotificationPermission by mutableStateOf(false)
        private set

    var isBatteryOptimizationDisabled by mutableStateOf(false)
        private set

    var showBackgroundLocationDialog by mutableStateOf(false)
        private set

    var showBatteryOptimizationDialog by mutableStateOf(false)
        private set

    private var permissionJustGranted by mutableStateOf(false)

    init {
        checkPermissionStatus()
    }

    private fun checkPermissionStatus() {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasBackgroundLocationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for API < 29
        }

        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for API < 33
        }

        // Check battery optimization status
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptimizationDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Not required for API < 23
        }
    }

    fun startLocationTrackingIfPermitted() {
        if (hasLocationPermission && hasBackgroundLocationPermission) {
            serviceHelper.startLocationTracking()
        }
    }

    fun handleLocationPermissionResult(permissions: Map<String, Boolean>) {
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        hasLocationPermission = granted

        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission) {
            // Show dialog to explain background location permission
            showBackgroundLocationDialog = true
        } else if (granted) {
            // Check for notification permission next
            checkAndRequestNotificationPermission()
        }
    }

    fun handleBackgroundLocationPermissionResult(granted: Boolean) {
        hasBackgroundLocationPermission = granted
        showBackgroundLocationDialog = false

        if (granted && hasLocationPermission) {
            // Check for notification permission next
            checkAndRequestNotificationPermission()
        }
    }

    fun handleNotificationPermissionResult(granted: Boolean) {
        hasNotificationPermission = granted

        // After notification permission, check battery optimization
        checkAndRequestBatteryOptimization()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            // Notification permission will be requested by the UI
            return
        } else {
            // If notification permission not needed or already granted, check battery optimization
            checkAndRequestBatteryOptimization()
        }
    }

    private fun checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimizationDisabled) {
            showBatteryOptimizationDialog = true
        } else if (hasLocationPermission && hasBackgroundLocationPermission) {
            // All permissions granted, start tracking
            permissionJustGranted = true
            startLocationTrackingIfPermitted()
        }
    }

    fun dismissBackgroundLocationDialog() {
        showBackgroundLocationDialog = false
    }

    fun dismissBatteryOptimizationDialog() {
        showBatteryOptimizationDialog = false
        // If user dismisses, still start tracking if location permissions are granted
        if (hasLocationPermission && hasBackgroundLocationPermission) {
            startLocationTrackingIfPermitted()
        }
    }

    fun requestBackgroundLocationPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        showBackgroundLocationDialog = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun requestNotificationPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openBatteryOptimizationSettings() {
        showBatteryOptimizationDialog = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // If direct battery optimization intent fails, open app info
            openAppInfo()
        }
    }

    fun openAppInfo() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general settings
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    }

    fun shouldRequestLocationPermission(): Boolean {
        return !hasLocationPermission
    }

    fun shouldRequestNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission
    }

    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun getNotificationPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            ""
        }
    }

    fun isAllPermissionsGranted(): Boolean {
        return hasLocationPermission && hasBackgroundLocationPermission && hasNotificationPermission
    }

    fun isAllPermissionsAndOptimizationsSet(): Boolean {
        return hasLocationPermission && hasBackgroundLocationPermission &&
                hasNotificationPermission && isBatteryOptimizationDisabled
    }

    // Additional helper method to refresh permission status
    fun refreshPermissionStatus() {
        checkPermissionStatus()
    }
}

@Composable
fun rememberPermissionManager(
    serviceHelper: ServiceHelper
): PermissionManager {
    val context = LocalContext.current
    return remember { PermissionManager(context, serviceHelper) }
}