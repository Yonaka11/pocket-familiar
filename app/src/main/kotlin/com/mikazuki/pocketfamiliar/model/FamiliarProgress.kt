package com.mikazuki.pocketfamiliar.model

enum class FamiliarInterest {
    MUSIC, WALKING, FOOD, SLEEP, PLAY, READING, CHARGING, NIGHT
}

enum class TouchInteraction {
    TAP, DOUBLE_TAP, PET, TICKLE, THROW, CATCH, JUGGLE, SOFT_CATCH
}

data class FamiliarPreferences(
    val favoriteInterests: Set<FamiliarInterest> = emptySet(),
    val favoriteTouch: Set<TouchInteraction> = emptySet(),
    val lessPreferredInterests: Set<FamiliarInterest> = emptySet(),
    val lessPreferredTouch: Set<TouchInteraction> = emptySet(),
)

data class FamiliarProgress(
    val familiarId: String,
    val bondXp: Int = 0,
    val playXp: Int = 0,
    val charms: Int = 0,
    val lifetimeSteps: Int = 0,
    val bestJuggleCombo: Int = 0,
) {
    val level: Int get() = levelForXp(bondXp)
    val playLevel: Int get() = levelForXp(playXp)

    companion object {
        fun levelForXp(xp: Int): Int {
            var level = 1
            var remaining = xp.coerceAtLeast(0)
            var cost = 100
            while (remaining >= cost && level < 50) {
                remaining -= cost
                level++
                cost = 100 + (level - 1) * 35
            }
            return level
        }
    }
}

data class FamiliarReward(
    val bondXp: Int = 0,
    val playXp: Int = 0,
    val charms: Int = 0,
    val preferenceBonusApplied: Boolean = false,
)
