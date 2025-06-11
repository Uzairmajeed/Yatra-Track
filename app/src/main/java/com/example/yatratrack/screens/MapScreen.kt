package com.example.yatratrack.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.yatratrack.helper.LocationData
import com.example.yatratrack.helper.LocationRepository
import com.example.yatratrack.helper.WorkManagerHelper
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val locationRepository = remember { LocationRepository(context) }
    val workManagerHelper = remember { WorkManagerHelper(context) }

    var locations by remember { mutableStateOf<List<LocationData>>(emptyList()) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasBackgroundLocationPermission by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var shouldFetchLocation by remember { mutableStateOf(false) }
    var permissionJustGranted by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0), // or MaterialTheme.colorScheme.primary
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

        // Load existing locations first
        locations = locationRepository.getAllLocations()

        if (hasLocationPermission && hasBackgroundLocationPermission) {
            // Only fetch location immediately if this is the very first app launch
            if (isFirstAppLaunch()) {
                isLoadingLocation = true
                workManagerHelper.fetchLocationImmediately()

                // Mark as launched so this won't happen again
                markAppAsLaunched()

                // Wait a bit for the immediate location to be processed
                delay(3000)
                locations = locationRepository.getAllLocations()
                isLoadingLocation = false
            }

            // Always ensure periodic tracking is running (this is safe to call multiple times)
            workManagerHelper.startLocationTracking()
        }
    }

    // Auto-refresh locations every 30 seconds when both permissions are granted
    LaunchedEffect(hasLocationPermission, hasBackgroundLocationPermission) {
        if (hasLocationPermission && hasBackgroundLocationPermission) {
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
            workManagerHelper.fetchLocationImmediately()
            delay(3000)
            locations = locationRepository.getAllLocations()
            isLoadingLocation = false
            shouldFetchLocation = false
        }
    }

    // Handle permission just granted (this is when user grants permission for the first time)
    LaunchedEffect(permissionJustGranted) {
        if (permissionJustGranted && hasLocationPermission && hasBackgroundLocationPermission) {
            isLoadingLocation = true
            workManagerHelper.fetchLocationImmediately()
            workManagerHelper.startLocationTracking()

            // Mark as launched since we're fetching location now
            markAppAsLaunched()

            delay(3000)
            locations = locationRepository.getAllLocations()
            isLoadingLocation = false
            permissionJustGranted = false
        }
    }

    // Basic location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        hasLocationPermission = granted

        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission) {
            // Show dialog to explain background location permission
            showBackgroundLocationDialog = true
        } else if (granted && hasBackgroundLocationPermission) {
            // Trigger the LaunchedEffect to handle permission granted
            permissionJustGranted = true
        }
    }

    // Background location permission launcher
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocationPermission = granted
        if (granted && hasLocationPermission) {
            workManagerHelper.startLocationTracking()
            permissionJustGranted = true
        }
    }

    // Background Location Permission Dialog
    if (showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            title = { Text("Background Location Required") },
            text = {
                Text("For continuous location tracking even when the app is closed, please allow 'Allow all the time' when prompted for location permission.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundLocationDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackgroundLocationDialog = false }
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
            .background(Color(0xFF1565C0)) // same as status bar color
    )
    Column(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding() // This ensures content starts below status bar
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
            // Permission Card
            if (!hasLocationPermission || !hasBackgroundLocationPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Location Permissions Required",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            text = when {
                                !hasLocationPermission -> "Basic location permission is required for tracking."
                                !hasBackgroundLocationPermission -> "Background location permission is needed for continuous tracking when app is closed."
                                else -> ""
                            },
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (!hasLocationPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else if (!hasBackgroundLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    showBackgroundLocationDialog = true
                                }
                            }
                        ) {
                            Text(
                                text = when {
                                    !hasLocationPermission -> "Grant Location Permission"
                                    !hasBackgroundLocationPermission -> "Grant Background Permission"
                                    else -> "Grant Permission"
                                }
                            )
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
            if (hasLocationPermission && hasBackgroundLocationPermission) {
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
                            .height(48.dp), // ensures uniform height
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
            if (hasLocationPermission && hasBackgroundLocationPermission) {
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
                        items(locations.reversed()) { location ->
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