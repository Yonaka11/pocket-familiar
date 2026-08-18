package com.mikazuki.pocketfamiliar.model

/** One-time skill milestones that reward playful interaction without gating progression. */
data class FamiliarAchievement(
    val id: String,
    val title: String,
    val description: String,
    val bonusPlayXp: Int = 0,
    val bonusBondXp: Int = 0,
)

object FamiliarAchievements {
    val NICE_CATCH = FamiliarAchievement("nice_catch", "Nice Catch", "Catch your familiar in mid-air.", 10, 3)
    val ORBIT_ACHIEVED = FamiliarAchievement("orbit_achieved", "Orbit Achieved", "Keep a familiar airborne for 5 seconds before catching it.", 20, 5)
    val DOUBLE_BOUNCE = FamiliarAchievement("double_bounce", "Double Bounce", "Catch a familiar after it has bounced off both screen sides.", 25, 5)
    val COMBO_FIVE = FamiliarAchievement("combo_five", "Juggle x5", "Reach a five-catch juggle combo.", 20, 5)
    val COMBO_TEN = FamiliarAchievement("combo_ten", "Juggle x10", "Reach a ten-catch juggle combo.", 40, 10)
    val BOOP_MASTER = FamiliarAchievement("boop_master", "Boop Certified", "Boop your familiar 25 times.", 15, 10)

    val all = listOf(NICE_CATCH, ORBIT_ACHIEVED, DOUBLE_BOUNCE, COMBO_FIVE, COMBO_TEN, BOOP_MASTER)
}
