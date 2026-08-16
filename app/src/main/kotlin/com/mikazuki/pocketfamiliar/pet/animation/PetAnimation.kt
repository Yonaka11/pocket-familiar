package com.mikazuki.pocketfamiliar.pet.animation

/**
 * One renderable animation frame.
 *
 * [Resource] keeps the existing one-drawable-per-frame path used by the built-in
 * familiars. [Atlas] lets character packs keep many 64x64 frames inside one
 * compact bitmap while still selecting individual frames at runtime.
 */
sealed interface PetFrame {
    data class Resource(val resId: Int) : PetFrame

    data class Atlas(
        val resId: Int,
        val frameIndex: Int,
        val columns: Int = 4,
        val rows: Int = 3,
    ) : PetFrame
}

/**
 * Describes a single looping or one-shot animation.
 *
 * @param frames Ordered list of renderable frames.
 * @param frameDurationMs How long each frame is displayed, in milliseconds.
 * @param loop Whether the animation should repeat indefinitely.
 */
data class PetAnimation(
    val frames: List<PetFrame>,
    val frameDurationMs: Long = 200L,
    val loop: Boolean = true,
) {
    init {
        require(frames.isNotEmpty()) { "PetAnimation must have at least one frame." }
    }

    /** Total duration of one full cycle in milliseconds. */
    val cycleDurationMs: Long get() = frames.size * frameDurationMs
}

/** Convenience helper for traditional drawable-per-frame animations. */
fun resourceFrames(vararg resIds: Int): List<PetFrame> =
    resIds.map(PetFrame::Resource)

/**
 * Convenience helper for a 4x3 sprite atlas. The supplied indices are read
 * left-to-right, top-to-bottom.
 */
fun atlasFrames(atlasResId: Int, vararg indices: Int): List<PetFrame> =
    indices.map { PetFrame.Atlas(atlasResId, it) }
