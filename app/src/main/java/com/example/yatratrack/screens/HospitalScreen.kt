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

data class Hospital(
    val name: String,
    val contact: String,
    val district: String
)

@Composable
fun HospitalScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color(0xFF1565C0), darkIcons = false)
    }

    val hospitals = listOf(
        Hospital("Sub District Hospital Bijbehara", "9906717273", "Anantnag"),
        Hospital("Sub District Hospital Dooru", "9906682666", "Anantnag"),
        Hospital("District Hospital Budgam", "9419004855", "Budgam"),
        Hospital("Sub District Hospital Char-i-Sharif", "7889316049", "Budgam")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(Color(0xFF1565C0))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nearby Hospitals",
                    fontSize = 20.sp,
                    color = Color.Black
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            items(hospitals) { hospital ->
                HospitalCard(hospital)
            }
        }
    }
}

@Composable
fun HospitalCard(hospital: Hospital) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(hospital.name, fontSize = 16.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Contact: ${hospital.contact}", fontSize = 14.sp, color = Color.DarkGray)
            Text("District: ${hospital.district}", fontSize = 14.sp, color = Color.Gray)
        }
    }
}
