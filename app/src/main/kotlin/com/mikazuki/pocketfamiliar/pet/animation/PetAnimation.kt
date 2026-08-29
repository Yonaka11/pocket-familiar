package com.mikazuki.pocketfamiliar.pet.animation

/**
 * One renderable animation frame.
 *
 * [Resource] keeps the existing one-drawable-per-frame path used by the built-in
 * familiars. [Atlas] lets character packs keep many frames inside one compact
 * bitmap while still selecting individual frames at runtime.
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
 * Convenience helper for sprite atlases. Indices are read left-to-right,
 * top-to-bottom. Defaults preserve the original 4x3 atlas layout while newer
 * packs may provide a different grid, such as EMK's 4x4 runtime atlases.
 */
fun atlasFrames(
    atlasResId: Int,
    vararg indices: Int,
    columns: Int = 4,
    rows: Int = 3,
): List<PetFrame> {
    require(columns > 0 && rows > 0) { "Atlas grid must be at least 1x1." }
    val maxFrames = columns * rows
    require(indices.all { it in 0 until maxFrames }) {
        "Atlas frame index must be inside a ${columns}x${rows} grid."
    }
    return indices.map { PetFrame.Atlas(atlasResId, it, columns, rows) }
}

/**
 * Frames from a one-row horizontal sprite strip.
 *
 * EMK Runtime V2 deliberately stores each action in a separate strip so walk,
 * run, jump/land, sleep and signature moves can have different frame counts
 * without wasting cells or forcing all behavior into a tiny universal atlas.
 */
fun stripFrames(stripResId: Int, frameCount: Int): List<PetFrame> {
    require(frameCount > 0) { "Sprite strip must contain at least one frame." }
    return (0 until frameCount).map { index ->
        PetFrame.Atlas(
            resId = stripResId,
            frameIndex = index,
            columns = frameCount,
            rows = 1,
        )
    }
}
