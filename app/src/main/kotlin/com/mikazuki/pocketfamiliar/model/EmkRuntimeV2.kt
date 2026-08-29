package com.mikazuki.pocketfamiliar.model

import android.content.Context
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.stripFrames

/**
 * Runtime bridge for the new individual EMK animation strips.
 *
 * Resource names are resolved dynamically so the code remains buildable before
 * the final binary art is copied into drawable-nodpi. Once a complete V2 pack is
 * packaged, the same app build path automatically upgrades that attendant from
 * the legacy 4x4 atlas to genuine multi-frame action strips.
 */
object EmkRuntimeV2 {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

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

    /** Returns null unless the character has a complete packaged Runtime V2 strip set. */
    fun profileOrNull(base: PetProfile): PetProfile? {
        val context = appContext ?: return null
        val spec = specs[base.id] ?: return null
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

        // All-or-nothing activation prevents a half-copied asset export from
        // leaving a character with missing walk/sleep/special states.
        val idle = resolve(spec.idle) ?: return null
        val walk = resolve(spec.walk) ?: return null
        val run = resolve(spec.run) ?: return null
        val jumpLand = resolve(spec.jumpLand) ?: return null
        val special = resolve(spec.special) ?: return null
        val reaction = resolve(spec.reaction) ?: return null
        val sleep = resolve(spec.sleep) ?: return null

        return base.copy(
            description = when (base.id) {
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
