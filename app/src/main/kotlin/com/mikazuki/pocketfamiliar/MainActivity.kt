package com.mikazuki.pocketfamiliar

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mikazuki.pocketfamiliar.story.ui.PocketFamiliarStoryHost
import com.mikazuki.pocketfamiliar.ui.theme.PocketFamiliarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show any crash that was recorded during the previous session.
        // This lets the user report the exact error without needing Logcat.
        showSavedCrashIfAny()

        enableEdgeToEdge()

        setContent {
            PocketFamiliarTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding(),
                ) {
                    PocketFamiliarStoryHost()
                }
            }
        }
    }

    private fun showSavedCrashIfAny() {
        val prefs = getSharedPreferences(PocketFamiliarApplication.CRASH_PREFS, Context.MODE_PRIVATE)
        val crash = prefs.getString(PocketFamiliarApplication.KEY_LAST_CRASH, null) ?: return
        prefs.edit().remove(PocketFamiliarApplication.KEY_LAST_CRASH).apply()

        AlertDialog.Builder(this)
            .setTitle("Crash report")
            .setMessage(crash)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
