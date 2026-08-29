package com.mikazuki.pocketfamiliar.model

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.atlasFrames
import com.mikazuki.pocketfamiliar.pet.animation.resourceFrames
import com.mikazuki.pocketfamiliar.pet.physics.FamiliarPhysicsProfile

object PetRegistry {

    val all: List<PetProfile> by lazy {
        listOf(
            familiar(),
            moonwing(),
            emi(),
            kaelani(),
            mira(),
            shinobuKocho(),
            shinobuOshino(),
        )
    }

    fun getById(id: String): PetProfile = all.find { it.id == id } ?: all.first()

    /**
     * EMK form choice is manual. There is deliberately no clock/day-night switch.
     * Attendant form automatically uses the new individual Runtime V2 strips when
     * the complete character pack is packaged; otherwise it safely falls back to
     * the existing 4x4 atlas. Familiar form continues using the spirit fallback
     * until dedicated familiar-form animation strips are drawn.
     */
    fun getRuntimeProfile(id: String, useFamiliarForm: Boolean): PetProfile {
        if (!useFamiliarForm) {
            val base = getById(id)
            return EmkRuntimeV2.profileOrNull(base) ?: base
        }
        return when (id) {
            "emi" -> staticFamiliarProfile(emi(), R.drawable.emi_night, "Sparkborn Trickster")
            "kaelani" -> staticFamiliarProfile(kaelani(), R.drawable.kaelani_night, "Bloom Spirit")
            "mira" -> staticFamiliarProfile(mira(), R.drawable.mira_night, "Dreamwatch Scholar")
            else -> getById(id)
        }
    }

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

    private val emiBehavior = FamiliarBehaviorProfile(
        sleepWeight = 0.04f, eatWeight = 0.05f, groomWeight = 0.03f,
        happyWeight = 0.12f, walkWeight = 0.30f, runWeight = 0.46f,
        idleDelayMs = 900L..2_500L,
    )

    private val kaelaniBehavior = FamiliarBehaviorProfile(
        sleepWeight = 0.10f, eatWeight = 0.08f, groomWeight = 0.14f,
        happyWeight = 0.10f, walkWeight = 0.46f, runWeight = 0.12f,
        idleDelayMs = 1_600L..4_200L,
    )

    private val miraBehavior = FamiliarBehaviorProfile(
        sleepWeight = 0.28f, eatWeight = 0.12f, groomWeight = 0.10f,
        happyWeight = 0.07f, walkWeight = 0.34f, runWeight = 0.09f,
        idleDelayMs = 2_200L..5_500L,
    )

    /** EMK attendant forms use the legacy 4x4 atlas as the V2 fallback. */
    private fun emi() = pixelAtlasProfile(
        id = "emi",
        displayName = "Emi",
        description = "Playful tech-royal attendant · kinetic pixel familiar.",
        previewResId = R.drawable.emi_avatar,
        atlasResId = R.drawable.emi_runtime_pixel_atlas,
        behavior = emiBehavior,
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.PLAY, FamiliarInterest.MUSIC),
            favoriteTouch = setOf(TouchInteraction.BOOP, TouchInteraction.DOUBLE_TAP, TouchInteraction.JUGGLE, TouchInteraction.CATCH),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.72f, gravityScale = 0.90f, airDrag = 0.72f, restitution = 0.42f),
    )

    private fun kaelani() = pixelAtlasProfile(
        id = "kaelani",
        displayName = "Kaelani",
        description = "Graceful bloom attendant · flowing pixel familiar.",
        previewResId = R.drawable.kaelani_avatar,
        atlasResId = R.drawable.kaelani_runtime_pixel_atlas,
        behavior = kaelaniBehavior,
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.MUSIC, FamiliarInterest.NIGHT, FamiliarInterest.PLAY),
            favoriteTouch = setOf(TouchInteraction.PET, TouchInteraction.TAP),
            lessPreferredTouch = setOf(TouchInteraction.JUGGLE),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.68f, gravityScale = 0.82f, airDrag = 0.80f, restitution = 0.30f),
    )

    private fun mira() = pixelAtlasProfile(
        id = "mira",
        displayName = "Mira",
        description = "Cozy red-haired scholar attendant · bookish pixel familiar.",
        previewResId = R.drawable.mira_avatar,
        atlasResId = R.drawable.mira_runtime_pixel_atlas,
        behavior = miraBehavior,
        preferences = FamiliarPreferences(
            favoriteInterests = setOf(FamiliarInterest.SLEEP, FamiliarInterest.FOOD, FamiliarInterest.READING),
            favoriteTouch = setOf(TouchInteraction.PET, TouchInteraction.TAP),
            lessPreferredTouch = setOf(TouchInteraction.JUGGLE),
        ),
        physics = FamiliarPhysicsProfile(mass = 0.90f, gravityScale = 1.0f, airDrag = 0.65f, restitution = 0.28f),
    )

    private fun pixelAtlasProfile(
        id: String,
        displayName: String,
        description: String,
        previewResId: Int,
        atlasResId: Int,
        behavior: FamiliarBehaviorProfile,
        preferences: FamiliarPreferences,
        physics: FamiliarPhysicsProfile,
    ): PetProfile {
        fun anim(duration: Long, vararg frames: Int) = PetAnimation(
            atlasFrames(atlasResId, *frames, columns = 4, rows = 4),
            duration,
        )

        // Legacy atlas contract: row 1 idle/orientation, row 2 walk, row 3 run, row 4 specials.
        val idle = anim(420, 0, 1)
        val walk = anim(155, 4, 5, 6, 7)
        val run = anim(105, 8, 9, 10, 11)
        val happy = anim(240, 12)
        val startled = anim(140, 13)
        val sleep = anim(1_100, 14)
        val signature = anim(260, 15)

        return PetProfile(
            id = id,
            displayName = displayName,
            description = description,
            previewResId = previewResId,
            idleAnim = idle,
            walkAnim = walk,
            runAnim = run,
            sleepAnim = sleep,
            fallAnim = startled,
            climbAnim = walk,
            jumpAnim = startled,
            holdAnim = startled,
            throwAnim = startled,
            hardLandAnim = startled,
            recoverAnim = idle,
            eatAnim = happy,
            groomAnim = signature,
            happyAnim = happy,
            musicAnim = signature,
            stepAnim = run,
            chargingAnim = sleep,
            lowBatteryAnim = sleep,
            deepSleepAnim = sleep,
            preferences = preferences,
            physics = physics,
            behavior = behavior,
        )
    }

    private fun staticFamiliarProfile(base: PetProfile, spiritResId: Int, formName: String): PetProfile {
        fun motion(duration: Long) = PetAnimation(resourceFrames(spiritResId), duration)
        return base.copy(
            displayName = "${base.displayName} · $formName",
            description = "$formName familiar form · manually selected, never clock-triggered.",
            previewResId = spiritResId,
            idleAnim = motion(420),
            walkAnim = motion(155),
            runAnim = motion(105),
            sleepAnim = motion(1_100),
            fallAnim = motion(120),
            climbAnim = motion(180),
            jumpAnim = motion(120),
            holdAnim = motion(180),
            throwAnim = motion(100),
            hardLandAnim = motion(160),
            recoverAnim = motion(300),
            eatAnim = motion(400),
            groomAnim = motion(380),
            happyAnim = motion(220),
            musicAnim = motion(220),
            stepAnim = motion(120),
            chargingAnim = motion(1_000),
            lowBatteryAnim = motion(900),
            deepSleepAnim = motion(1_300),
            proceduralMotion = true,
        )
    }

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
