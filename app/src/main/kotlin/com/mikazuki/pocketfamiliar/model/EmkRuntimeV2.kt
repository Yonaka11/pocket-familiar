package com.mikazuki.pocketfamiliar.model

import android.content.Context
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.atlasFrames

/**
 * Runtime bridge for the clean EMK Runtime V2 character atlases.
 *
 * Each character owns one transparent 4x7 production atlas. Frames were isolated
 * individually from the locked character boards before assembly, so animation
 * cells never rely on broad poster slicing that can clip hair, effects, or limbs.
 *
 * Atlas rows:
 * 0 = idle / blink (4)
 * 1 = walk (4)
 * 2 = run (4)
 * 3 = jump / land (4)
 * 4 = signature special (4)
 * 5 = reaction / recover (3 useful frames; fourth cell is padding)
 * 6 = sleep / rest (2 useful frames; remaining cells are padding)
 */
object EmkRuntimeV2 {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private data class CharacterSpec(
        val atlasResourceName: String,
        val idleMs: Long,
        val walkMs: Long,
        val runMs: Long,
        val jumpLandMs: Long,
        val specialMs: Long,
        val reactionMs: Long,
        val sleepMs: Long,
    )

    private val specs = mapOf(
        "emi" to CharacterSpec(
            atlasResourceName = "emi_runtime_v2_atlas",
            idleMs = 300,
            walkMs = 145,
            runMs = 95,
            jumpLandMs = 120,
            specialMs = 140,
            reactionMs = 180,
            sleepMs = 720,
        ),
        "kaelani" to CharacterSpec(
            atlasResourceName = "kaelani_runtime_v2_atlas",
            idleMs = 360,
            walkMs = 165,
            runMs = 112,
            jumpLandMs = 135,
            specialMs = 175,
            reactionMs = 260,
            sleepMs = 760,
        ),
        "mira" to CharacterSpec(
            atlasResourceName = "mira_runtime_v2_atlas",
            idleMs = 380,
            walkMs = 172,
            runMs = 118,
            jumpLandMs = 138,
            specialMs = 180,
            reactionMs = 275,
            sleepMs = 800,
        ),
    )

    /** Returns null unless the character's clean Runtime V2 atlas is packaged. */
    fun profileOrNull(base: PetProfile): PetProfile? {
        val context = appContext ?: return null
        val spec = specs[base.id] ?: return null
        val resources = context.resources

        @Suppress("DEPRECATION")
        val atlasResId = resources.getIdentifier(
            spec.atlasResourceName,
            "drawable",
            context.packageName,
        )
        if (atlasResId == 0) return null

        fun anim(duration: Long, loop: Boolean = true, vararg frames: Int) = PetAnimation(
            frames = atlasFrames(
                atlasResId,
                *frames,
                columns = 4,
                rows = 7,
            ),
            frameDurationMs = duration,
            loop = loop,
        )

        val idle = anim(spec.idleMs, true, 0, 1, 2, 3)
        val walk = anim(spec.walkMs, true, 4, 5, 6, 7)
        val run = anim(spec.runMs, true, 8, 9, 10, 11)
        val jumpLand = anim(spec.jumpLandMs, false, 12, 13, 14, 15)
        val special = anim(spec.specialMs, false, 16, 17, 18, 19)
        val reaction = anim(spec.reactionMs, false, 20, 21, 22)
        val sleep = anim(spec.sleepMs, true, 24, 25)

        return base.copy(
            description = when (base.id) {
                "emi" -> "Tech-royal attendant · clean multi-frame Runtime V2."
                "kaelani" -> "Bloom attendant · clean multi-frame Runtime V2."
                "mira" -> "Red-haired scholar attendant · clean multi-frame Runtime V2."
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
