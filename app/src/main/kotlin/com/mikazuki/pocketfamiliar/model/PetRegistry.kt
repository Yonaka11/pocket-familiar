package com.mikazuki.pocketfamiliar.model

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation

/**
 * Central registry of all built-in pet profiles.
 *
 * To add a custom sprite pack:
 *  1. Add drawable resources for each animation state.
 *  2. Create a new [PetProfile] entry here.
 *  3. The profile is immediately available in the pet selector.
 */
object PetRegistry {

    val all: List<PetProfile> by lazy { listOf(familiar(), moonwing()) }

    fun getById(id: String): PetProfile = all.find { it.id == id } ?: all.first()

    // ── Familiar ─────────────────────────────────────────────────────────────

    private fun familiar() = PetProfile(
        id = "familiar",
        displayName = "Familiar",
        description = "A mischievous purple cat-spirit.",
        previewResId = R.drawable.ic_pet_idle,
        idleAnim = PetAnimation(listOf(R.drawable.ic_pet_idle), 500),
        walkAnim = PetAnimation(listOf(R.drawable.ic_pet_walk1, R.drawable.ic_pet_walk2), 160),
        runAnim  = PetAnimation(listOf(R.drawable.ic_pet_run1, R.drawable.ic_pet_run2,
                                       R.drawable.ic_pet_run1, R.drawable.ic_pet_run2), 110),
        sleepAnim = PetAnimation(listOf(R.drawable.ic_pet_sleep, R.drawable.ic_pet_sleep2), 1200),
        fallAnim  = PetAnimation(listOf(R.drawable.ic_pet_fall), 100),
        climbAnim = PetAnimation(listOf(R.drawable.ic_pet_climb), 300),
        jumpAnim  = PetAnimation(listOf(R.drawable.ic_pet_jump), 100),
    )

    // ── Moonwing ─────────────────────────────────────────────────────────────

    private fun moonwing() = PetProfile(
        id = "moonwing",
        displayName = "Moonwing",
        description = "A graceful butterfly spirit.",
        previewResId = R.drawable.ic_moonwing_idle,
        idleAnim  = PetAnimation(listOf(R.drawable.ic_moonwing_idle), 600),
        walkAnim  = PetAnimation(listOf(R.drawable.ic_moonwing_walk1, R.drawable.ic_moonwing_walk2), 200),
        runAnim   = PetAnimation(listOf(R.drawable.ic_moonwing_run1, R.drawable.ic_moonwing_run2,
                                        R.drawable.ic_moonwing_run1, R.drawable.ic_moonwing_run2), 120),
        sleepAnim = PetAnimation(listOf(R.drawable.ic_moonwing_sleep, R.drawable.ic_moonwing_sleep2), 1400),
        fallAnim  = PetAnimation(listOf(R.drawable.ic_moonwing_fall), 100),
        climbAnim = PetAnimation(listOf(R.drawable.ic_moonwing_climb), 250),
        jumpAnim  = PetAnimation(listOf(R.drawable.ic_moonwing_jump), 100),
    )
}
