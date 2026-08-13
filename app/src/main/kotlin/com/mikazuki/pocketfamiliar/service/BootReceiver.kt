package com.mikazuki.pocketfamiliar.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mikazuki.pocketfamiliar.data.PetSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "BootReceiver"

/**
 * Restarts [PetOverlayService] after device reboot if the user opted in.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != "android.intent.action.QUICKBOOT_POWERON") return

        val repo = PetSettingsRepository(context)
        // goAsync() keeps the receiver alive while we do the async settings read
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repo.settingsFlow.first()
                if (settings.autoStartOnBoot) {
                    Log.d(TAG, "Auto-start on boot: starting service")
                    val serviceIntent = Intent(context, PetOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
