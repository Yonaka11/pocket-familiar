package com.mikazuki.pocketfamiliar.story.model

import androidx.annotation.DrawableRes

enum class StoryAccent {
    ELECTRIC,
    BLOOM,
    SCHOLAR,
    CELESTIAL,
    UNKNOWN,
}

enum class StoryTransition {
    CUT,
    FADE,
    GLITCH,
    SLIDE_LEFT,
    SLIDE_UP,
    WHITE_FLASH,
    PAGE_TURN,
    PETAL_REVEAL,
}

enum class StoryInteractionType {
    CLEAR_STATIC,
    HOLD_SIGNAL,
    TRACE_GLYPH,
}

enum class StoryPanelTreatment {
    STANDARD,
    COMIC_INK,
    GLITCHED,
    SILHOUETTE,
}

data class StoryEpisode(
    val id: String,
    val title: String,
    val subtitle: String,
    val memoryRewardId: String,
    val memoryRewardTitle: String,
    /** Story rewards can belong to a specific attendant even if another familiar is selected. */
    val focusFamiliarId: String? = null,
    val beats: List<StoryBeat>,
)

sealed interface StoryBeat {
    val id: String
    val autoAdvanceMs: Long? get() = null

    data class Panel(
        override val id: String,
        @DrawableRes val imageResId: Int,
        val caption: String? = null,
        val accent: StoryAccent = StoryAccent.CELESTIAL,
        val transition: StoryTransition = StoryTransition.CUT,
        val cameraZoom: Float = 1.0f,
        val treatment: StoryPanelTreatment = StoryPanelTreatment.COMIC_INK,
    ) : StoryBeat

    data class Dialogue(
        override val id: String,
        val speaker: String,
        val text: String,
        val accent: StoryAccent,
        val transition: StoryTransition = StoryTransition.FADE,
        val thought: Boolean = false,
    ) : StoryBeat

    /** Procedural ink-and-halftone panel used for Seraphi / unknown presences. */
    data class InkShadow(
        override val id: String,
        val caption: String? = null,
        val accent: StoryAccent = StoryAccent.UNKNOWN,
        val transition: StoryTransition = StoryTransition.GLITCH,
        val halo: Boolean = false,
        val looming: Boolean = false,
    ) : StoryBeat

    data class Flash(
        override val id: String,
        val accent: StoryAccent = StoryAccent.CELESTIAL,
        override val autoAdvanceMs: Long = 120L,
    ) : StoryBeat

    data class Interaction(
        override val id: String,
        val type: StoryInteractionType,
        val prompt: String,
        val completionText: String,
        val accent: StoryAccent,
    ) : StoryBeat

    data class MemoryUnlock(
        override val id: String,
        val title: String,
        val description: String,
        val accent: StoryAccent = StoryAccent.CELESTIAL,
    ) : StoryBeat

    data class End(
        override val id: String,
        override val autoAdvanceMs: Long = 650L,
    ) : StoryBeat
}

data class StoryProgress(
    val completedEpisodeIds: Set<String> = emptySet(),
    val memoryFragmentIds: Set<String> = emptySet(),
) {
    val memoryCount: Int get() = memoryFragmentIds.size

    fun hasCompleted(episodeId: String): Boolean = episodeId in completedEpisodeIds
}
