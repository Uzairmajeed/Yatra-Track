package com.example.yatratrack.screens


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
import androidx.navigation.NavController
import com.example.yatratrack.R
import com.example.yatratrack.helper.WorkManagerHelper
import com.example.yatratrack.helper.rememberPermissionManager
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

data class MenuOption(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    val workManagerHelper = remember { WorkManagerHelper(context) }
    val permissionManager = rememberPermissionManager(workManagerHelper)
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0),
            darkIcons = false
        )
    }

    // Start location tracking if permissions are already granted
    LaunchedEffect(Unit) {
        permissionManager.startLocationTrackingIfPermitted()
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

    // Request permission when screen loads (if not already granted)
    LaunchedEffect(permissionManager.hasLocationPermission) {
        if (permissionManager.shouldRequestLocationPermission()) {
            permissionLauncher.launch(permissionManager.getLocationPermissions())
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
        // Header Section with Logo and Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.headerlogo), // Replace with your logo drawable
                    contentDescription = "Amarnath Yatra Logo",
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Title
                Text(
                    text = "Amarnath Yatra 2025",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0)
                )
            }
        }

        // Header Image Carousel Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            // Image list for carousel
            val carouselImages = listOf(
                R.drawable.amarnath_cave,
                R.drawable.image1,
                R.drawable.image2,
                R.drawable.image3
            )

            EnhancedImageCarousel(
                images = carouselImages,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Permission status card
        if (!permissionManager.isAllPermissionsGranted()) {
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
                            !permissionManager.hasLocationPermission -> "Basic location permission is required"
                            !permissionManager.hasBackgroundLocationPermission -> "Background location permission needed for continuous tracking"
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = Color(0xFF1976D2),
                    shape = RoundedCornerShape(8.dp)
                )
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
                    text = "© 2025 Amarnath Yatra App \nAn Initiative By ICCC Srinagar",
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
            .height(110.dp)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedImageCarousel(
    images: List<Int>,
    modifier: Modifier = Modifier
) {
    // Start from the second image (index 1)
    val pagerState = rememberPagerState(
        initialPage = 1,
        pageCount = { images.size }
    )

    // Auto-scroll effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000) // Change image every 4 seconds
            val nextPage = (pagerState.currentPage + 1) % images.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        // Enhanced Carousel with side previews - HIGHLY VISIBLE SIDE IMAGES
        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp), // Reduced from 200.dp to 140.dp
            contentPadding = PaddingValues(horizontal = 60.dp), // More padding = more visible sides
            pageSpacing = 6.dp // Less spacing = closer images
        ) { page ->
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Calculate the offset from the current page
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                ).absoluteValue

                        // Apply scaling - side images are 80% size (very visible)
                        scaleX = lerp(
                            start = 0.8f, // Much larger side images
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        scaleY = lerp(
                            start = 0.8f, // Much larger side images
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )

                        // Apply alpha - side images are 85% opacity (highly visible)
                        alpha = lerp(
                            start = 0.85f, // Much more visible
                            stop = 1.0f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (page == pagerState.currentPage) 8.dp else 4.dp
                )
            ) {
                Box {
                    Image(
                        painter = painterResource(id = images[page]),
                        contentDescription = "Amarnath Yatra Image ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay for better contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.2f) // Lighter overlay
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }
            }
        }

        // Enhanced Dot Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            images.forEachIndexed { index, _ ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(
                            width = if (isSelected) 24.dp else 8.dp,
                            height = 8.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) Color(0xFF1565C0)
                            else Color(0xFFBDBDBD)
                        )
                        .animateContentSize(
                            animationSpec = tween(300)
                        )
                )
                if (index < images.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
