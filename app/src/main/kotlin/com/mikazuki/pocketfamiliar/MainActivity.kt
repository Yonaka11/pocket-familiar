package com.mikazuki.pocketfamiliar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mikazuki.pocketfamiliar.ui.screens.HomeScreen
import com.mikazuki.pocketfamiliar.ui.theme.PocketFamiliarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PocketFamiliarTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding(),
                ) {
                    HomeScreen()
                }
            }
        }
    }
}
