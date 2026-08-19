package com.mikazuki.pocketfamiliar.model

/**
 * Snapshot of user-controlled pet settings.
 * Exposed as a data class so callers can compare instances cheaply.
 */
data class PetSettings(
    /** Scale factor for the pet sprite, range 0.5–2.0. */
    val petSize: Float = 1.0f,
    /** Pixels per second the pet walks. */
    val movementSpeed: Float = 80f,
    /** Whether the pet is allowed to enter the SLEEP state. */
    val sleepEnabled: Boolean = true,
    /** Whether the service should restart automatically after device boot. */
    val autoStartOnBoot: Boolean = false,
    /** Identifier for the selected pet sprite pack. */
    val selectedPetId: String = "default",
    /** Identifier for the active earned cosmetic screen theme. */
    val selectedThemeId: String = FamiliarThemeCatalog.DEFAULT_THEME_ID,
    /** Debug-only theme ids that can be stacked regardless of unlock state. */
    val debugThemeIds: Set<String> = emptySet(),
)
