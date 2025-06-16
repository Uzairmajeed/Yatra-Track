package com.example.yatratrack.screens

import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.yatratrack.helper.LocationData
import com.example.yatratrack.helper.LocationRepository
import com.example.yatratrack.helper.ServiceHelper
import com.example.yatratrack.helper.rememberPermissionManager
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val locationRepository = remember { LocationRepository(context) }
    val serviceHelper = remember { ServiceHelper(context) }
    val permissionManager = rememberPermissionManager(serviceHelper)

    var locations by remember { mutableStateOf<List<LocationData>>(emptyList()) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var shouldFetchLocation by remember { mutableStateOf(false) }
    var permissionJustGranted by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0),
            darkIcons = false
        )
    }

    // Check if this is the first time the app is launched
    fun isFirstAppLaunch(): Boolean {
        val prefs = context.getSharedPreferences("yatra_prefs", android.content.Context.MODE_PRIVATE)
        return !prefs.getBoolean("has_launched_before", false)
    }

    // Mark that the app has been launched
    fun markAppAsLaunched() {
        val prefs = context.getSharedPreferences("yatra_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_launched_before", true).apply()
    }

    // Check initial permission status and handle first-time location fetch
    LaunchedEffect(Unit) {
        // Load existing locations first
        locations = locationRepository.getAllLocations()

        if (permissionManager.isAllPermissionsGranted()) {
            // Only fetch location immediately if this is the very first app launch
            if (isFirstAppLaunch()) {
                isLoadingLocation = true
                serviceHelper.fetchLocationImmediately()

                // Mark as launched so this won't happen again
                markAppAsLaunched()

                // Wait a bit for the immediate location to be processed
                delay(3000)
                locations = locationRepository.getAllLocations()
                isLoadingLocation = false
            }

            // Check if tracking is already active, restart if needed
            if (!serviceHelper.isLocationTrackingActive()) {
                Log.d("MapScreen", "Starting location tracking")
                serviceHelper.startLocationTracking()
            } else {
                Log.d("MapScreen", "Location tracking already active")
            }
        }
    }

    // Auto-refresh locations every 30 seconds when both permissions are granted
    LaunchedEffect(permissionManager.hasLocationPermission, permissionManager.hasBackgroundLocationPermission) {
        if (permissionManager.isAllPermissionsGranted()) {
            while (true) {
                delay(30000) // 30 seconds
                locations = locationRepository.getAllLocations()
            }
        }
    }

    // Handle manual location fetch
    LaunchedEffect(shouldFetchLocation) {
        if (shouldFetchLocation) {
            isLoadingLocation = true
            serviceHelper.fetchLocationImmediately()
            delay(3000)
            locations = locationRepository.getAllLocations()
            isLoadingLocation = false
            shouldFetchLocation = false
        }
    }

    // Handle permission just granted (this is when user grants permission for the first time)
    LaunchedEffect(permissionJustGranted) {
        if (permissionJustGranted && permissionManager.isAllPermissionsGranted()) {
            isLoadingLocation = true
            serviceHelper.fetchLocationImmediately()
            serviceHelper.startLocationTracking()

            // Mark as launched since we're fetching location now
            markAppAsLaunched()

            delay(3000)
            locations = locationRepository.getAllLocations()
            isLoadingLocation = false
            permissionJustGranted = false
        }
    }

    // Permission launcher for basic location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionManager.handleLocationPermissionResult(permissions)
    }

    // Background location permission launcher
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionManager.handleBackgroundLocationPermissionResult(granted)
    }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionManager.handleNotificationPermissionResult(granted)
    }

    // Request location permission when screen loads (if not already granted)
    LaunchedEffect(permissionManager.hasLocationPermission) {
        if (permissionManager.shouldRequestLocationPermission()) {
            permissionLauncher.launch(permissionManager.getLocationPermissions())
        }
    }

    // Request notification permission after location permissions are handled
    LaunchedEffect(
        permissionManager.hasLocationPermission,
        permissionManager.hasBackgroundLocationPermission
    ) {
        if (permissionManager.hasLocationPermission &&
            permissionManager.hasBackgroundLocationPermission &&
            permissionManager.shouldRequestNotificationPermission()) {
            notificationPermissionLauncher.launch(permissionManager.getNotificationPermission())
        }
    }

    // Background Location Permission Dialog
    if (permissionManager.showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { permissionManager.dismissBackgroundLocationDialog() },
            title = { Text("Background Location Required") },
            text = {
                Text("For continuous location tracking even when the app is closed, please allow 'Allow all the time' when prompted for location permission.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionManager.requestBackgroundLocationPermission(backgroundLocationLauncher)
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { permissionManager.dismissBackgroundLocationDialog() }
                ) {
                    Text("Skip")
                }
            }
        )
    }

    // Battery Optimization Dialog
    if (permissionManager.showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { permissionManager.dismissBatteryOptimizationDialog() },
            title = { Text("Battery Optimization Settings") },
            text = {
                Text("To ensure continuous location tracking, please disable battery optimization for this app. This will prevent the system from stopping the app in the background.\n\nGo to: App Info → Battery → Allow background activity")
            },
            confirmButton = {
                TextButton(
                    onClick = { permissionManager.openAppInfo() }
                ) {
                    Text("Open App Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { permissionManager.dismissBatteryOptimizationDialog() }
                ) {
                    Text("Skip")
                }
            }
        )
    }

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .background(Color(0xFF1565C0))
    )

    Column(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Location Tracking") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        locations = locationRepository.getAllLocations()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Enhanced permission status card (same as main screen)
            if (!permissionManager.isAllPermissionsAndOptimizationsSet()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "App Permissions & Settings",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )

                        val statusText = when {
                            !permissionManager.hasLocationPermission -> "✗ Location permission required"
                            !permissionManager.hasBackgroundLocationPermission -> "✗ Background location permission needed"
                            !permissionManager.hasNotificationPermission -> "✗ Notification permission needed"
                            !permissionManager.isBatteryOptimizationDisabled -> "✗ Battery optimization should be disabled"
                            else -> "✓ All permissions granted"
                        }

                        Text(
                            text = statusText,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )

                        // Show individual permission status
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            PermissionStatusRow("Location", permissionManager.hasLocationPermission)
                            PermissionStatusRow("Background Location", permissionManager.hasBackgroundLocationPermission)
                            PermissionStatusRow("Notifications", permissionManager.hasNotificationPermission)
                            PermissionStatusRow("Battery Optimization", permissionManager.isBatteryOptimizationDisabled)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading indicator when fetching initial location
            if (isLoadingLocation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fetching current location...",
                            color = Color(0xFF7B1FA2)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Stats Card
            if (permissionManager.isAllPermissionsGranted()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Locations Tracked",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Updates every 15 minutes",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "${locations.size}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            locations = locationRepository.getAllLocations()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Refresh",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            shouldFetchLocation = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Get Now",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            locationRepository.clearAllLocations()
                            locations = emptyList()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Clear All",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Locations List
            if (permissionManager.isAllPermissionsGranted()) {
                if (locations.isNotEmpty()) {
                    Text(
                        text = "Recent Locations (${locations.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = locations.size,
                            key = { index -> locations.reversed()[index].timestamp }
                        ) { index ->
                            val location = locations.reversed()[index]
                            LocationItem(location = location)
                        }
                    }

                } else if (!isLoadingLocation) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No locations tracked yet",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Location tracking is active, locations will appear here",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LocationItem(location: LocationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lat: ${String.format("%.6f", location.latitude)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Lng: ${String.format("%.6f", location.longitude)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = location.timestamp,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                location.accuracy?.let { accuracy ->
                    Text(
                        text = "Accuracy: ${accuracy.toInt()}m",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}