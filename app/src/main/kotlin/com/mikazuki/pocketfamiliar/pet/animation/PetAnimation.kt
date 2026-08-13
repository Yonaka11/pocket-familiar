package com.mikazuki.pocketfamiliar.pet.animation

/**
 * Describes a single looping or one-shot animation as a sequence of drawable resource IDs.
 *
 * @param frames       Ordered list of drawable resource IDs.
 * @param frameDurationMs How long each frame is displayed, in milliseconds.
 * @param loop         Whether the animation should repeat indefinitely.
 */
data class PetAnimation(
    val frames: List<Int>,
    val frameDurationMs: Long = 200L,
    val loop: Boolean = true,
) {
    init {
        require(frames.isNotEmpty()) { "PetAnimation must have at least one frame." }
    }

    /** Total duration of one full cycle in milliseconds. */
    val cycleDurationMs: Long get() = frames.size * frameDurationMs
}
