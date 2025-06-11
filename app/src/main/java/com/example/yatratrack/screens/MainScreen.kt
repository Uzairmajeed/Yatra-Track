package com.example.yatratrack.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.yatratrack.R
import com.example.yatratrack.helper.WorkManagerHelper
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

data class MenuOption(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val workManagerHelper = remember { WorkManagerHelper(context) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasBackgroundLocationPermission by remember { mutableStateOf(false) }
    var permissionJustGranted by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0),
            darkIcons = false
        )
    }

    // Check location permission status and handle initial setup
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

        if (hasLocationPermission && hasBackgroundLocationPermission) {
            // Always ensure periodic tracking is running (this is safe to call multiple times)
            workManagerHelper.startLocationTracking()
        }
    }

    // Handle permission just granted (this is when user grants permission for the first time)
    LaunchedEffect(permissionJustGranted) {
        if (permissionJustGranted) {
            if (hasLocationPermission && hasBackgroundLocationPermission) {
                workManagerHelper.startLocationTracking()
            }
            permissionJustGranted = false
        }
    }

    // Permission launcher for basic location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        hasLocationPermission = granted

        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission) {
            // Show dialog to explain background location permission
            showBackgroundLocationDialog = true
        } else if (granted) {
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
        }
    }

    // Request permission when screen loads (if not already granted)
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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

    val menuOptions = listOf(
//        MenuOption(
//            title = "Battery\nOptimization",
//            icon = Icons.Default.Settings
//        ) {
//            // Open battery optimization settings
//            try {
//                val intent = Intent()
//                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
//                intent.data = Uri.parse("package:${context.packageName}")
//                context.startActivity(intent)
//            } catch (e: Exception) {
//                selectedOption = "Please disable battery optimization for this app in Settings > Apps > Yatra Track > Battery"
//            }
//        },

        MenuOption(
            title = "Nearby\nHospitals",
            icon = Icons.Default.Home
        ) { selectedOption = "Nearby Hospitals - Data not available for now" },

        MenuOption(
            title = "Nearby Langers",
            icon = Icons.Default.CheckCircle
        ) { selectedOption = "Nearby Langers - Data not available for now" },

        MenuOption(
            title = "Nearby Police\nStations",
            icon = Icons.Default.Home
        ) { selectedOption = "Nearby Police Stations - Data not available for now" },

        MenuOption(
            title = "Emergency\nContacts",
            icon = Icons.Default.Call
        ) { selectedOption = "Emergency Contacts - Data not available for now" },

        MenuOption(
            title = "Interesting\nPlaces",
            icon = Icons.Default.Place
        ) { selectedOption = "Interesting Places - Data not available for now" },

        MenuOption(
            title = "Map",
            icon = Icons.Default.LocationOn
        ) { navController.navigate("map") }
    )

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .background(Color(0xFF1565C0))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.amarnath_cave),
                contentDescription = "Amarnath Cave",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Permission status card
        if (!hasLocationPermission || !hasBackgroundLocationPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Location Permissions Required",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        text = when {
                            !hasLocationPermission -> "Basic location permission is required"
                            !hasBackgroundLocationPermission -> "Background location permission needed for continuous tracking"
                            else -> ""
                        },
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Show selected option message if any
        selectedOption?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF1976D2),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Menu Grid Section
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(menuOptions) { option ->
                MenuCard(
                    option = option,
                    onClick = option.onClick
                )
            }
        }

        // Footer Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "© 2025 Amarnath Yatra App | Developed by MSP",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MenuCard(
    option: MenuOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = option.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = Color(0xFF1976D2),
                lineHeight = 16.sp
            )
        }
    }
}