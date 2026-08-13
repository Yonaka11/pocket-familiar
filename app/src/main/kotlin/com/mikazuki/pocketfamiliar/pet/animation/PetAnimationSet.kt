package com.mikazuki.pocketfamiliar.pet.animation

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.pet.behavior.PetState

/**
 * Maps each [PetState] to its [PetAnimation].
 *
 * Walk directions reuse the same two frames; horizontal flipping is handled
 * by [PetView] via canvas.scale(-1, 1) so we don't need duplicate art.
 */
object PetAnimationSet {

    private val idle = PetAnimation(
        frames = listOf(R.drawable.ic_pet_idle),
        frameDurationMs = 500L,
    )

    private val walk = PetAnimation(
        frames = listOf(R.drawable.ic_pet_walk1, R.drawable.ic_pet_walk2),
        frameDurationMs = 160L,
    )

    // Two sleep frames create a gentle "breathing" effect
    private val sleep = PetAnimation(
        frames = listOf(R.drawable.ic_pet_sleep, R.drawable.ic_pet_sleep2),
        frameDurationMs = 1200L,
    )

    private val fall = PetAnimation(
        frames = listOf(R.drawable.ic_pet_fall),
        frameDurationMs = 100L,
    )

    fun forState(state: PetState): PetAnimation = when (state) {
        is PetState.Idle -> idle
        is PetState.WalkLeft -> walk
        is PetState.WalkRight -> walk
        is PetState.Sleep -> sleep
        is PetState.Falling -> fall
        is PetState.Dragged -> fall
    }

    /**
     * Returns true when the sprite should be drawn mirrored horizontally.
     * Only WalkLeft requires a flip — all other states face right by default.
     */
    fun isFlipped(state: PetState): Boolean = state is PetState.WalkLeft
}
