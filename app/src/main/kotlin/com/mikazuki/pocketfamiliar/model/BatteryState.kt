package com.mikazuki.pocketfamiliar.model

/**
 * Snapshot of device battery state.
 * Intended to drive future battery-reactive pet behavior.
 */
data class BatteryState(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isLow: Boolean = false,
) {
    /** Semantic mood based on battery level and charging state. */
    val mood: BatteryMood
        get() = when {
            isCharging -> BatteryMood.CHARGING
            levelPercent >= 80 -> BatteryMood.HAPPY
            levelPercent >= 40 -> BatteryMood.NORMAL
            levelPercent >= 15 -> BatteryMood.TIRED
            else -> BatteryMood.SLEEPY
        }
}

enum class BatteryMood {
    HAPPY, NORMAL, TIRED, SLEEPY, CHARGING
}
