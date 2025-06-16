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

data class EmergencyContact(
    val title: String,
    val phone: String,
    val agency: String
)

@Composable
fun EmergencyContactsScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color(0xFF1565C0),
            darkIcons = false
        )
    }

    val contacts = listOf(
        EmergencyContact("Ambulance", "102", "Health Services"),
        EmergencyContact("Fire Control Room", "101", "Disaster Response"),
        EmergencyContact("Police Emergency", "112", "J&K Police"),
        EmergencyContact("Yatra Helpline", "1800-180-7135", "Shri Amarnath Shrine Board")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ✅ Status bar spacer with blue background
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .background(Color(0xFF1565C0))
        )

        // ✅ Custom header like MainScreen
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
                    text = "Emergency Contacts",
                    fontSize = 20.sp,
                    color = Color.Black,

                )
            }
        }

        // ✅ Emergency contact list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            items(contacts) { contact ->
                EmergencyCard(contact)
            }
        }
    }
}

@Composable
fun EmergencyCard(contact: EmergencyContact) {
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
                text = contact.title,
                fontSize = 16.sp,
                color = Color(0xFFD32F2F),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Phone: ${contact.phone}",
                fontSize = 14.sp,
                color = Color.DarkGray
            )
            Text(
                text = "Agency: ${contact.agency}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
