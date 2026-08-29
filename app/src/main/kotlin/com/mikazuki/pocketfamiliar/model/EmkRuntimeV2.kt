package com.mikazuki.pocketfamiliar.model

import android.content.Context
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.stripFrames

/**
 * Runtime bridge for the new individual EMK animation strips.
 *
 * The resources are resolved by name instead of compile-time R references so the
 * branch remains buildable before the final binary art is copied into
 * drawable-nodpi. As soon as the full strip set for a character is present, the
 * runtime automatically upgrades that attendant from the legacy 4x4 atlas to V2.
 */
object EmkRuntimeV2 {

    private data class StripSpec(
        val resourceName: String,
        val frames: Int,
        val frameMs: Long,
        val loop: Boolean = true,
    )

    private data class CharacterSpec(
        val idle: StripSpec,
        val walk: StripSpec,
        val run: StripSpec,
        val jumpLand: StripSpec,
        val special: StripSpec,
        val reaction: StripSpec,
        val sleep: StripSpec,
    )

    private val specs = mapOf(
        "emi" to CharacterSpec(
            idle = StripSpec("emi_anim_idle", 4, 300),
            walk = StripSpec("emi_anim_walk", 4, 145),
            run = StripSpec("emi_anim_run", 4, 95),
            jumpLand = StripSpec("emi_anim_jump_land", 4, 120, loop = false),
            special = StripSpec("emi_anim_special", 4, 140, loop = false),
            reaction = StripSpec("emi_anim_recover", 3, 180, loop = false),
            sleep = StripSpec("emi_anim_sleep", 2, 720),
        ),
        "kaelani" to CharacterSpec(
            idle = StripSpec("kaelani_anim_idle", 4, 360),
            walk = StripSpec("kaelani_anim_walk", 4, 165),
            run = StripSpec("kaelani_anim_run", 4, 112),
            jumpLand = StripSpec("kaelani_anim_jump_land", 4, 135, loop = false),
            special = StripSpec("kaelani_anim_special", 4, 175, loop = false),
            reaction = StripSpec("kaelani_anim_happy", 3, 260, loop = false),
            sleep = StripSpec("kaelani_anim_sleep", 2, 760),
        ),
        "mira" to CharacterSpec(
            idle = StripSpec("mira_anim_idle", 4, 380),
            walk = StripSpec("mira_anim_walk", 4, 172),
            run = StripSpec("mira_anim_run", 4, 118),
            jumpLand = StripSpec("mira_anim_jump_land", 4, 138, loop = false),
            special = StripSpec("mira_anim_special", 4, 180, loop = false),
            reaction = StripSpec("mira_anim_happy", 3, 275, loop = false),
            sleep = StripSpec("mira_anim_sleep", 2, 800),
        ),
    )

    /**
     * Returns the best available runtime profile.
     * Familiar-form selection remains manual and continues using the existing
     * spirit-form fallback until dedicated familiar-form frame packs are ready.
     */
    fun profile(context: Context, id: String, useFamiliarForm: Boolean): PetProfile {
        if (useFamiliarForm) return PetRegistry.getRuntimeProfile(id, true)

        val base = PetRegistry.getById(id)
        val spec = specs[id] ?: return base
        val resources = context.resources
        val packageName = context.packageName

        fun resolve(strip: StripSpec): PetAnimation? {
            @Suppress("DEPRECATION")
            val resId = resources.getIdentifier(strip.resourceName, "drawable", packageName)
            if (resId == 0) return null
            return PetAnimation(
                frames = stripFrames(resId, strip.frames),
                frameDurationMs = strip.frameMs,
                loop = strip.loop,
            )
        }

        // Upgrade only when the complete required pack exists. A half-copied art
        // export must never leave the character with missing runtime states.
        val idle = resolve(spec.idle) ?: return base
        val walk = resolve(spec.walk) ?: return base
        val run = resolve(spec.run) ?: return base
        val jumpLand = resolve(spec.jumpLand) ?: return base
        val special = resolve(spec.special) ?: return base
        val reaction = resolve(spec.reaction) ?: return base
        val sleep = resolve(spec.sleep) ?: return base

        return base.copy(
            description = when (id) {
                "emi" -> "Tech-royal attendant · multi-frame Runtime V2."
                "kaelani" -> "Bloom attendant · multi-frame Runtime V2."
                "mira" -> "Red-haired scholar attendant · multi-frame Runtime V2."
                else -> base.description
            },
            idleAnim = idle,
            walkAnim = walk,
            runAnim = run,
            sleepAnim = sleep,
            fallAnim = jumpLand,
            climbAnim = walk,
            jumpAnim = jumpLand,
            holdAnim = jumpLand,
            throwAnim = jumpLand,
            hardLandAnim = jumpLand,
            recoverAnim = reaction,
            eatAnim = reaction,
            groomAnim = reaction,
            happyAnim = reaction,
            musicAnim = special,
            stepAnim = run,
            chargingAnim = sleep,
            lowBatteryAnim = sleep,
            deepSleepAnim = sleep,
            proceduralMotion = false,
        )
    }
}
