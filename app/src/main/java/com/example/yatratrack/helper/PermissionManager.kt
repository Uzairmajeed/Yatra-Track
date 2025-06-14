package com.example.yatratrack.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class PermissionManager(
    private val context: Context,
    private val workManagerHelper: WorkManagerHelper
) {
    var hasLocationPermission by mutableStateOf(false)
        private set

    var hasBackgroundLocationPermission by mutableStateOf(false)
        private set

    var showBackgroundLocationDialog by mutableStateOf(false)
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
    }

    fun startLocationTrackingIfPermitted() {
        if (hasLocationPermission && hasBackgroundLocationPermission) {
            workManagerHelper.startLocationTracking()
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
            // Trigger permission granted handling
            permissionJustGranted = true
            startLocationTrackingIfPermitted()
        }
    }

    fun handleBackgroundLocationPermissionResult(granted: Boolean) {
        hasBackgroundLocationPermission = granted
        if (granted && hasLocationPermission) {
            workManagerHelper.startLocationTracking()
        }
    }

    fun dismissBackgroundLocationDialog() {
        showBackgroundLocationDialog = false
    }

    fun requestBackgroundLocationPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        showBackgroundLocationDialog = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            launcher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun shouldRequestLocationPermission(): Boolean {
        return !hasLocationPermission
    }

    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun isAllPermissionsGranted(): Boolean {
        return hasLocationPermission && hasBackgroundLocationPermission
    }
}

@Composable
fun rememberPermissionManager(
    workManagerHelper: WorkManagerHelper
): PermissionManager {
    val context = LocalContext.current
    return remember { PermissionManager(context, workManagerHelper) }
}