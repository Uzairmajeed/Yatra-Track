package com.example.yatratrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

data class Langar(
    val name: String,
    val location: String,
    val timing: String
)

@Composable
fun LangarScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0),
            darkIcons = false
        )
    }

    val langars = listOf(
        Langar("Shri Amarnath Langar Seva", "Nunwan Base Camp", "5:00 AM - 10:00 PM"),
        Langar("Sai Baba Langar", "Baltal Base", "24 Hours"),
        Langar("Golden Trust Langar", "Domail Checkpost", "6:00 AM - 9:00 PM"),
        Langar("Seva Bharti Langar", "Panchtarni", "5:30 AM - 8:00 PM")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ✅ Status bar background color
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(Color(0xFF1565C0))
        )

        // ✅ Custom header (no TopAppBar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nearby Langers",
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }

        // ✅ List of langars (unchanged)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            items(langars) { langar ->
                LangarCard(langar)
            }
        }
    }
}

@Composable
fun LangarCard(langar: Langar) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = langar.name,
                fontSize = 16.sp,
                color = Color(0xFF1565C0),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Location: ${langar.location}",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Timings: ${langar.timing}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
