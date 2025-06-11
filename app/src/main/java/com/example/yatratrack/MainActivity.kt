package com.example.yatratrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.yatratrack.navigation.AppNavHost
import com.example.yatratrack.ui.theme.YatraTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YatraTrackTheme {
                AppNavHost()
            }
        }
    }

}

