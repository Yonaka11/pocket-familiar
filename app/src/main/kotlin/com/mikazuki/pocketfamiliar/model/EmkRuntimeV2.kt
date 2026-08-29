package com.mikazuki.pocketfamiliar.model

import android.content.Context
import android.graphics.BitmapFactory
import com.mikazuki.pocketfamiliar.pet.animation.PetAnimation
import com.mikazuki.pocketfamiliar.pet.animation.stripFrames

/**
 * Runtime bridge for the clean EMK Runtime V2 per-action sprite strips.
 *
 * V2 only activates when every required strip for a character exists, decodes,
 * and has an equal-cell layout. A partial or damaged pack is ignored so it can
 * never silently replace a working legacy profile with a still-image fallback.
 */
object EmkRuntimeV2 {

    private var appContext: Context? = null
    private val packCache = mutableMapOf<String, ResolvedPack?>()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        packCache.clear()
    }

    private data class CharacterSpec(
        val reactionName: String,
        val idleMs: Long,
        val walkMs: Long,
        val runMs: Long,
        val jumpLandMs: Long,
        val specialMs: Long,
        val reactionMs: Long,
        val sleepMs: Long,
    )

    private data class ResolvedPack(
        val idle: Int,
        val walk: Int,
        val run: Int,
        val jumpLand: Int,
        val special: Int,
        val reaction: Int,
        val sleep: Int,
    )

    private val specs = mapOf(
        "emi" to CharacterSpec(
            reactionName = "recover",
            idleMs = 300,
            walkMs = 145,
            runMs = 95,
            jumpLandMs = 120,
            specialMs = 140,
            reactionMs = 180,
            sleepMs = 720,
        ),
        "kaelani" to CharacterSpec(
            reactionName = "happy",
            idleMs = 360,
            walkMs = 165,
            runMs = 112,
            jumpLandMs = 135,
            specialMs = 175,
            reactionMs = 260,
            sleepMs = 760,
        ),
        "mira" to CharacterSpec(
            reactionName = "happy",
            idleMs = 380,
            walkMs = 172,
            runMs = 118,
            jumpLandMs = 138,
            specialMs = 180,
            reactionMs = 275,
            sleepMs = 800,
        ),
    )

    /** Human-readable status used by Character Lab. */
    fun statusFor(characterId: String): String = when {
        characterId !in specs -> "Built-in runtime"
        resolvePack(characterId) != null -> "EMK V2 · multi-frame strips"
        else -> "Legacy/fallback art · V2 pack invalid"
    }

    /** Returns null unless the character's complete, decodable V2 strip pack is packaged. */
    fun profileOrNull(base: PetProfile): PetProfile? {
        val spec = specs[base.id] ?: return null
        val pack = resolvePack(base.id) ?: return null

        fun anim(resId: Int, count: Int, duration: Long, loop: Boolean = true) = PetAnimation(
            frames = stripFrames(resId, count),
            frameDurationMs = duration,
            loop = loop,
        )

        val idle = anim(pack.idle, 4, spec.idleMs)
        val walk = anim(pack.walk, 4, spec.walkMs)
        val run = anim(pack.run, 4, spec.runMs)
        val jumpLand = anim(pack.jumpLand, 4, spec.jumpLandMs, loop = false)
        val special = anim(pack.special, 4, spec.specialMs, loop = false)
        val reaction = anim(pack.reaction, 3, spec.reactionMs, loop = false)
        val sleep = anim(pack.sleep, 2, spec.sleepMs)

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

    private fun resolvePack(characterId: String): ResolvedPack? {
        if (packCache.containsKey(characterId)) return packCache[characterId]

        val context = appContext ?: return null
        val spec = specs[characterId] ?: return null
        val resources = context.resources

        fun resolve(action: String, frames: Int): Int? {
            @Suppress("DEPRECATION")
            val resId = resources.getIdentifier(
                "${characterId}_anim_$action",
                "drawable",
                context.packageName,
            )
            if (resId == 0) return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            runCatching { BitmapFactory.decodeResource(resources, resId, options) }.getOrNull()
            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0 || width % frames != 0 || width / frames != height) return null
            return resId
        }

        val resolved = run {
            val idle = resolve("idle", 4) ?: return@run null
            val walk = resolve("walk", 4) ?: return@run null
            val run = resolve("run", 4) ?: return@run null
            val jumpLand = resolve("jump_land", 4) ?: return@run null
            val special = resolve("special", 4) ?: return@run null
            val reaction = resolve(spec.reactionName, 3) ?: return@run null
            val sleep = resolve("sleep", 2) ?: return@run null
            ResolvedPack(idle, walk, run, jumpLand, special, reaction, sleep)
        }

        packCache[characterId] = resolved
        return resolved
    }
}
