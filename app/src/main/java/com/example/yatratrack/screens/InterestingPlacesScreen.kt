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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

data class InterestingPlace(
    val name: String,
    val description: String,
    val location: String
)

@Composable
fun InterestingPlacesScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0),
            darkIcons = false
        )
    }

    val places = listOf(
        InterestingPlace("Amarnath Cave", "Holy Hindu shrine in the Himalayas", "Pahalgam Route"),
        InterestingPlace("Chandanwari", "Starting point of Amarnath Yatra", "Pahalgam"),
        InterestingPlace("Baltal Valley", "Alternate trek route to the cave", "Sonmarg"),
        InterestingPlace("Panchtarni", "Overnight camping point", "Between Baltal and Cave")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 🟦 Status bar background
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(Color(0xFF1565C0))
        )

        // 🔻 Custom header (like MainScreen)
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
                    text = "Interesting Places",
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }

        // 🗺️ List of interesting places
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            items(places) { place ->
                PlaceCard(place)
            }
        }
    }
}

@Composable
fun PlaceCard(place: InterestingPlace) {
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
                text = place.name,
                fontSize = 16.sp,
                color = Color(0xFF1565C0),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Location: ${place.location}",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                text = place.description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
