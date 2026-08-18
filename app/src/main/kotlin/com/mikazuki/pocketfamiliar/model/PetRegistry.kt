package com.mikazuki.pocketfamiliar.model

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.atlasFrames
import com.mikazuki.pocketfamiliar.pet.animation.resourceFrames
import com.mikazuki.pocketfamiliar.pet.physics.FamiliarPhysicsProfile

object PetRegistry {

    val all: List<PetProfile> by lazy {
        listOf(familiar(), moonwing(), shinobuKocho(), shinobuOshino())
    }

    fun getById(id: String): PetProfile = all.find { it.id == id } ?: all.first()

    private fun familiar() = PetProfile(
        id = "familiar",
        displayName = "Familiar",
        description = "A playful purple cat-spirit.",
        previewResId = R.drawable.ic_pet_idle,
        idleAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_idle), 500),
        walkAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_walk1, R.drawable.ic_pet_walk2), 160),
        runAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_run1, R.drawable.ic_pet_run2, R.drawable.ic_pet_run1, R.drawable.ic_pet_run2), 110),
        sleepAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_sleep, R.drawable.ic_pet_sleep2), 1200),
        fallAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_fall), 100),
        climbAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_climb), 300),
        jumpAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_jump), 100),
        holdAnim = PetAnimation(resourceFrames(R.drawable.ic_pet_jump), 180),
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.PLAY, FamiliarInterest.WALKING),
            favoriteTouch = setOf(TouchInteraction.PET, TouchInteraction.JUGGLE, TouchInteraction.CATCH),
            lessPreferredInterests = setOf(FamiliarInterest.READING),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.85f, restitution = 0.42f, airDrag = 0.58f),
    )

    private fun moonwing() = PetProfile(
        id = "moonwing",
        displayName = "Moonwing",
        description = "A graceful butterfly spirit.",
        previewResId = R.drawable.ic_moonwing_idle,
        idleAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_idle), 600),
        walkAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_walk1, R.drawable.ic_moonwing_walk2), 200),
        runAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_run1, R.drawable.ic_moonwing_run2, R.drawable.ic_moonwing_run1, R.drawable.ic_moonwing_run2), 120),
        sleepAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_sleep, R.drawable.ic_moonwing_sleep2), 1400),
        fallAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_fall), 100),
        climbAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_climb), 250),
        jumpAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_jump), 100),
        holdAnim = PetAnimation(resourceFrames(R.drawable.ic_moonwing_jump), 180),
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.MUSIC, FamiliarInterest.NIGHT, FamiliarInterest.SLEEP),
            favoriteTouch = setOf(TouchInteraction.PET),
            lessPreferredTouch = setOf(TouchInteraction.JUGGLE),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.55f, gravityScale = 0.78f, airDrag = 0.82f, restitution = 0.30f),
    )

    private fun shinobuKocho() = PetProfile(
        id = "shinobu_kocho",
        displayName = "Shinobu Kocho",
        description = "A graceful butterfly swordswoman.",
        previewResId = R.drawable.shinobu_kocho_preview,
        idleAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 0, 1), 420),
        walkAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 2, 3), 150),
        runAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 4, 5), 105),
        sleepAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 8, 9), 1100),
        fallAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 10), 100),
        climbAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 2, 3), 230),
        jumpAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 6, 7), 120),
        holdAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 7), 180),
        throwAnim = PetAnimation(atlasFrames(R.drawable.shinobu_kocho_atlas, 6, 7), 90),
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.WALKING, FamiliarInterest.READING),
            favoriteTouch = setOf(TouchInteraction.PET, TouchInteraction.CATCH),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.72f, gravityScale = 0.90f, airDrag = 0.72f, restitution = 0.34f),
    )

    private fun shinobuOshino() = PetProfile(
        id = "shinobu_oshino",
        displayName = "Shinobu Oshino",
        description = "A donut-loving vampire familiar.",
        previewResId = R.drawable.shinobu_oshino_preview,
        idleAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 0, 1), 450),
        walkAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 2, 3), 155),
        runAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 4, 5), 105),
        sleepAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 8, 9), 1150),
        fallAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 10), 100),
        climbAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 2, 3), 230),
        jumpAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 6, 7), 120),
        holdAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 7), 180),
        throwAnim = PetAnimation(atlasFrames(R.drawable.shinobu_oshino_atlas, 6, 7), 90),
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.FOOD, FamiliarInterest.NIGHT, FamiliarInterest.SLEEP),
            favoriteTouch = setOf(TouchInteraction.TAP),
            lessPreferredInterests = setOf(FamiliarInterest.WALKING),
            lessPreferredTouch = setOf(TouchInteraction.JUGGLE),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.95f, gravityScale = 1.02f, airDrag = 0.60f, restitution = 0.38f),
    )
}
