package com.mikazuki.pocketfamiliar.model

data class BossRestorationRequirements(
    val requiredBondLevelPerAttendant: Int = 10,
    val requiredPlayLevelPerAttendant: Int = 5,
    val requiredAchievementsPerAttendant: Int = 3,
    val requiredMemoriesPerAttendant: Int = 3,
)

data class AttendantStoryProgress(
    val familiarId: String,
    val bondLevel: Int,
    val playLevel: Int,
    val achievementCount: Int,
    val memoryCount: Int,
)

data class BossRestorationState(
    val requirements: BossRestorationRequirements = BossRestorationRequirements(),
    val attendants: List<AttendantStoryProgress> = emptyList(),
) {
    val canUnlockSeraphiAscendedSpirit: Boolean
        get() = attendants.size >= 3 && attendants.all {
            it.bondLevel >= 5 && it.achievementCount >= 1
        }

    val canRestoreSeraphiHumanForm: Boolean
        get() = attendants.size >= 3 && attendants.all {
            it.bondLevel >= requirements.requiredBondLevelPerAttendant &&
                it.playLevel >= requirements.requiredPlayLevelPerAttendant &&
                it.achievementCount >= requirements.requiredAchievementsPerAttendant &&
                it.memoryCount >= requirements.requiredMemoriesPerAttendant
        }
}

/**
 * Story rule: Seraphi Astrea is the boss of Emi, Kaelani, and Mira. She begins
 * sealed in her base spirit form. Progress with all three attendants restores
 * her power first as an Ascended Seraphim, then finally her true human form.
 */
object PocketFamiliarStory {
    const val BOSS_ID = "seraphi_astrea"
    val attendantIds = setOf("emi", "kaelani", "mira")

    const val premise =
        "Emi, Kaelani, and Mira are helping their boss, Seraphi Astrea, recover the human form she lost. " +
            "Bond, play, memories, gifts, activity, and achievements restore fragments of her celestial power."
}
