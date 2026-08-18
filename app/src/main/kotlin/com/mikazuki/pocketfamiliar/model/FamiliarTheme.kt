package com.mikazuki.pocketfamiliar.model

enum class ThemeCategory {
    ATMOSPHERE,
    FRAME,
    AURA,
}

enum class ThemeVisual {
    NONE,
    SAKURA_DRIFT,
    NEON_TECH_FRAME,
    COZY_STUDY_GLOW,
    CELESTIAL_HALO,
    MOON_DUST,
    BLOOM_GARDEN,
}

sealed interface ThemeUnlock {
    data object Starter : ThemeUnlock
    data class BondLevel(val level: Int) : ThemeUnlock
    data class FamiliarLevel(val level: Int) : ThemeUnlock
    data class AchievementCount(val count: Int) : ThemeUnlock
}

data class FamiliarTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val category: ThemeCategory,
    val visual: ThemeVisual,
    val unlock: ThemeUnlock,
    val signatureCharacterId: String? = null,
)

object FamiliarThemeCatalog {
    const val DEFAULT_THEME_ID = "none"

    val all = listOf(
        FamiliarTheme(
            id = DEFAULT_THEME_ID,
            displayName = "Clear Sky",
            description = "No screen effect.",
            category = ThemeCategory.ATMOSPHERE,
            visual = ThemeVisual.NONE,
            unlock = ThemeUnlock.Starter,
        ),
        FamiliarTheme(
            id = "sakura_drift",
            displayName = "Sakura Drift",
            description = "Soft petals tumble across the screen.",
            category = ThemeCategory.ATMOSPHERE,
            visual = ThemeVisual.SAKURA_DRIFT,
            unlock = ThemeUnlock.BondLevel(3),
            signatureCharacterId = "kaelani",
        ),
        FamiliarTheme(
            id = "neon_tech_frame",
            displayName = "Neon Tech Frame",
            description = "A sharp cyber HUD frames the display.",
            category = ThemeCategory.FRAME,
            visual = ThemeVisual.NEON_TECH_FRAME,
            unlock = ThemeUnlock.FamiliarLevel(4),
            signatureCharacterId = "emi",
        ),
        FamiliarTheme(
            id = "cozy_study_glow",
            displayName = "Cozy Study Glow",
            description = "Warm lamp-light motes gather at the edges.",
            category = ThemeCategory.AURA,
            visual = ThemeVisual.COZY_STUDY_GLOW,
            unlock = ThemeUnlock.BondLevel(5),
            signatureCharacterId = "mira",
        ),
        FamiliarTheme(
            id = "celestial_halo",
            displayName = "Celestial Halo",
            description = "Seraphic rings and starlight orbit the screen.",
            category = ThemeCategory.AURA,
            visual = ThemeVisual.CELESTIAL_HALO,
            unlock = ThemeUnlock.AchievementCount(3),
            signatureCharacterId = "seraphi_astrea",
        ),
        FamiliarTheme(
            id = "moon_dust",
            displayName = "Moon Dust",
            description = "Slow silver-blue motes drift like night pollen.",
            category = ThemeCategory.ATMOSPHERE,
            visual = ThemeVisual.MOON_DUST,
            unlock = ThemeUnlock.FamiliarLevel(7),
        ),
        FamiliarTheme(
            id = "bloom_garden",
            displayName = "Bloom Garden",
            description = "Petals, leaves, and tiny lights gather into a living border.",
            category = ThemeCategory.FRAME,
            visual = ThemeVisual.BLOOM_GARDEN,
            unlock = ThemeUnlock.AchievementCount(6),
            signatureCharacterId = "kaelani",
        ),
    )

    fun getById(id: String): FamiliarTheme = all.firstOrNull { it.id == id } ?: all.first()

    fun isUnlocked(theme: FamiliarTheme, progress: FamiliarProgress, achievementCount: Int): Boolean = when (val rule = theme.unlock) {
        ThemeUnlock.Starter -> true
        is ThemeUnlock.BondLevel -> progress.bondLevel >= rule.level
        is ThemeUnlock.FamiliarLevel -> progress.level >= rule.level
        is ThemeUnlock.AchievementCount -> achievementCount >= rule.count
    }

    fun unlockLabel(theme: FamiliarTheme): String = when (val rule = theme.unlock) {
        ThemeUnlock.Starter -> "Starter"
        is ThemeUnlock.BondLevel -> "Bond ${rule.level}"
        is ThemeUnlock.FamiliarLevel -> "Level ${rule.level}"
        is ThemeUnlock.AchievementCount -> "${rule.count} achievements"
    }
}
