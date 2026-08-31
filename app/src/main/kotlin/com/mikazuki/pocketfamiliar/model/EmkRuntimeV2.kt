package com.mikazuki.pocketfamiliar.model

import android.content.Context
import android.graphics.BitmapFactory
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.stripFrames

/**
 * Runtime bridge for the clean EMK animation-strip packs.
 *
 * A character only upgrades to V3 when every required strip exists AND can be
 * decoded with the expected horizontal frame count. A present-but-corrupt file
 * therefore cannot silently replace the legacy runtime again.
 */
object EmkRuntimeV2 {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    enum class RuntimeArtStatus { V3_STRIPS, LEGACY_FALLBACK }

    private data class Action(
        val suffix: String,
        val frames: Int,
        val frameMs: Long,
        val loop: Boolean = true,
    )

    private data class CharacterSpec(
        val idle: Action,
        val walk: Action,
        val run: Action,
        val jumpLand: Action,
        val special: Action,
        val reaction: Action,
        val sleep: Action,
    )

    private val specs = mapOf(
        "emi" to CharacterSpec(
            Action("idle", 4, 300),
            Action("walk", 6, 145),
            Action("run", 6, 95),
            Action("jump_land", 4, 120, false),
            Action("special", 6, 140, false),
            Action("recover", 4, 180, false),
            Action("sleep", 3, 720),
        ),
        "kaelani" to CharacterSpec(
            Action("idle", 4, 360),
            Action("walk", 6, 165),
            Action("run", 6, 112),
            Action("jump_land", 4, 135, false),
            Action("special", 6, 175, false),
            Action("happy", 4, 260, false),
            Action("sleep", 3, 760),
        ),
        "mira" to CharacterSpec(
            Action("idle", 4, 380),
            Action("walk", 6, 172),
            Action("run", 6, 118),
            Action("jump_land", 4, 138, false),
            Action("special", 6, 180, false),
            Action("happy", 4, 275, false),
            Action("sleep", 3, 800),
        ),
    )

    fun runtimeArtStatus(id: String): RuntimeArtStatus =
        if (resolvePack(id) != null) RuntimeArtStatus.V3_STRIPS else RuntimeArtStatus.LEGACY_FALLBACK

    fun profileOrNull(base: PetProfile): PetProfile? {
        val pack = resolvePack(base.id) ?: return null
        val spec = specs.getValue(base.id)

        fun anim(action: Action, resId: Int) = PetAnimation(
            frames = stripFrames(resId, action.frames),
            frameDurationMs = action.frameMs,
            loop = action.loop,
        )

        val idle = anim(spec.idle, pack.getValue(spec.idle.suffix))
        val walk = anim(spec.walk, pack.getValue(spec.walk.suffix))
        val run = anim(spec.run, pack.getValue(spec.run.suffix))
        val jumpLand = anim(spec.jumpLand, pack.getValue(spec.jumpLand.suffix))
        val special = anim(spec.special, pack.getValue(spec.special.suffix))
        val reaction = anim(spec.reaction, pack.getValue(spec.reaction.suffix))
        val sleep = anim(spec.sleep, pack.getValue(spec.sleep.suffix))

        return base.copy(
            description = when (base.id) {
                "emi" -> "Tech-royal attendant · multi-frame Runtime V3."
                "kaelani" -> "Bloom attendant · multi-frame Runtime V3."
                "mira" -> "Red-haired scholar attendant · multi-frame Runtime V3."
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

    private fun resolvePack(id: String): Map<String, Int>? {
        val context = appContext ?: return null
        val spec = specs[id] ?: return null
        val actions = listOf(spec.idle, spec.walk, spec.run, spec.jumpLand, spec.special, spec.reaction, spec.sleep)
        val result = linkedMapOf<String, Int>()

        for (action in actions) {
            @Suppress("DEPRECATION")
            val resId = context.resources.getIdentifier(
                "${id}_anim_${action.suffix}",
                "drawable",
                context.packageName,
            )
            if (resId == 0 || !validStrip(resId, action.frames)) return null
            result[action.suffix] = resId
        }
        return result
    }

    /** Validate bounds without allocating the bitmap. */
    private fun validStrip(resId: Int, frameCount: Int): Boolean {
        val context = appContext ?: return false
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return runCatching {
            context.resources.openRawResource(resId).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val width = options.outWidth
            val height = options.outHeight
            width > 0 && height > 0 && width % frameCount == 0 && width / frameCount == height
        }.getOrDefault(false)
    }
}
