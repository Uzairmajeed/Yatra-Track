package com.example.yatratrack.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.yatratrack.screens.EmergencyContactsScreen
import com.example.yatratrack.screens.HospitalScreen
import com.example.yatratrack.screens.InterestingPlacesScreen
import com.example.yatratrack.screens.LangarScreen
import com.example.yatratrack.screens.LoginScreen
import com.example.yatratrack.screens.MainScreen
import com.example.yatratrack.screens.MapScreen
import com.example.yatratrack.screens.PoliceStationScreen
import com.example.yatratrack.screens.RegisterScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object MapScreen: Screen("map")
    object Hospital : Screen("hospital")
    object PoliceStations : Screen("policeStations")
    object Langars : Screen("langars")
    object EmergencyContacts : Screen("emergencyContacts")
    object InterestingPlaces : Screen("interestingPlaces")
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("yatra_prefs", Context.MODE_PRIVATE)
    val isRegistered = sharedPreferences.getBoolean("isRegistered", false)

    val startDestination = if (isRegistered) {
        Screen.Main.route
    } else {
        Screen.Register.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Main.route) { MainScreen(navController) }
        composable(Screen.MapScreen.route) { MapScreen(navController) }
        composable(Screen.Hospital.route) { HospitalScreen(navController) }
        composable(Screen.PoliceStations.route) { PoliceStationScreen(navController) }
        composable(Screen.Langars.route) { LangarScreen(navController) }
        composable(Screen.EmergencyContacts.route) { EmergencyContactsScreen(navController) }
        composable(Screen.InterestingPlaces.route) { InterestingPlacesScreen(navController) }
    }
}