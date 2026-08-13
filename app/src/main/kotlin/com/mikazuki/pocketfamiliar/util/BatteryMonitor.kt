package com.mikazuki.pocketfamiliar.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.mikazuki.pocketfamiliar.model.BatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Listens to [Intent.ACTION_BATTERY_CHANGED] (a sticky broadcast; no permission
 * needed) and updates a [StateFlow] that the rest of the app observes.
 *
 * Register in [Context.registerReceiver] and unregister via [unregister] to
 * avoid leaks.
 */
class BatteryMonitor(private val context: Context) {

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent ?: return
            _batteryState.value = intent.toBatteryState()
        }
    }

    fun register() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        // Sticky broadcast — registerReceiver returns the last broadcast immediately
        val stickyIntent = context.registerReceiver(receiver, filter)
        stickyIntent?.let { _batteryState.value = it.toBatteryState() }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered — safe to ignore
        }
    }

    private fun Intent.toBatteryState(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct = if (scale > 0) (level * 100 / scale) else 0

        val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        val health = getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val low = pct <= 15

        return BatteryState(
            levelPercent = pct,
            isCharging = charging,
            isLow = low,
        )
    }
}
