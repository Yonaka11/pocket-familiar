package com.mikazuki.pocketfamiliar.story.data

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.story.model.StoryAccent
import com.mikazuki.pocketfamiliar.story.model.StoryBeat
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import com.mikazuki.pocketfamiliar.story.model.StoryInteractionType
import com.mikazuki.pocketfamiliar.story.model.StoryTransition

object StoryCatalog {

    const val SIGNAL_EPISODE_ID = "episode_00_the_signal"
    const val SIGNAL_MEMORY_ID = "memory_signal_fragment_00"

    fun signalEpisode(selectedFamiliarId: String): StoryEpisode {
        val cast = when (selectedFamiliarId) {
            "kaelani" -> CastFlavor(
                name = "Kaelani",
                imageResId = R.drawable.kaelani_avatar,
                spiritResId = R.drawable.kaelani_night,
                accent = StoryAccent.BLOOM,
                opening = "Something is blooming where nothing should be.",
                ending = "I remember that light... but I don't remember from where.",
            )
            "mira" -> CastFlavor(
                name = "Mira",
                imageResId = R.drawable.mira_avatar,
                spiritResId = R.drawable.mira_night,
                accent = StoryAccent.SCHOLAR,
                opening = "No. That line wasn't there before.",
                ending = "I know that pattern. I just don't remember learning it.",
            )
            else -> CastFlavor(
                name = "Emi",
                imageResId = R.drawable.emi_avatar,
                spiritResId = R.drawable.emi_night,
                accent = StoryAccent.ELECTRIC,
                opening = "Wait. Don't touch that.",
                ending = "That wasn't supposed to happen.",
            )
        }

        return StoryEpisode(
            id = SIGNAL_EPISODE_ID,
            title = "Episode 0 · The Signal",
            subtitle = "Something is moving behind the interface.",
            memoryRewardId = SIGNAL_MEMORY_ID,
            memoryRewardTitle = "Signal Fragment 00",
            beats = listOf(
                StoryBeat.Flash("signal_boot_flash", StoryAccent.CELESTIAL, 140L),
                StoryBeat.Panel(
                    id = "familiar_stops",
                    imageResId = cast.imageResId,
                    caption = "${cast.name} stops mid-motion and stares past the edge of the screen.",
                    accent = cast.accent,
                    transition = StoryTransition.GLITCH,
                    cameraZoom = 1.08f,
                ),
                StoryBeat.Dialogue(
                    id = "opening_warning",
                    speaker = cast.name,
                    text = cast.opening,
                    accent = cast.accent,
                    transition = StoryTransition.SLIDE_UP,
                ),
                StoryBeat.Panel(
                    id = "signal_crawls",
                    imageResId = cast.spiritResId,
                    caption = "A thread of light crawls behind the interface instead of across it.",
                    accent = cast.accent,
                    transition = StoryTransition.SLIDE_LEFT,
                    cameraZoom = 1.16f,
                ),
                StoryBeat.Dialogue(
                    id = "did_you_see_that",
                    speaker = cast.name,
                    text = "...did you see that?",
                    accent = cast.accent,
                    transition = StoryTransition.FADE,
                ),
                StoryBeat.Flash("seraphi_flash", StoryAccent.CELESTIAL, 100L),
                StoryBeat.Panel(
                    id = "seraphi_fragment",
                    imageResId = R.drawable.seraphi_launcher_foreground,
                    caption = "For less than a second, a broken halo resolves inside the static.",
                    accent = StoryAccent.CELESTIAL,
                    transition = StoryTransition.WHITE_FLASH,
                    cameraZoom = 1.24f,
                ),
                StoryBeat.Dialogue(
                    id = "seraphi_voice",
                    speaker = "???",
                    text = "Find me.",
                    accent = StoryAccent.CELESTIAL,
                    transition = StoryTransition.CUT,
                ),
                StoryBeat.Interaction(
                    id = "clear_static",
                    type = StoryInteractionType.CLEAR_STATIC,
                    prompt = "Drag across the static to stabilize the signal.",
                    completionText = "The interference breaks apart. Something underneath notices you.",
                    accent = cast.accent,
                ),
                StoryBeat.Panel(
                    id = "aftershock",
                    imageResId = cast.imageResId,
                    caption = "The signal collapses. ${cast.name} keeps staring at the place where it was.",
                    accent = cast.accent,
                    transition = StoryTransition.GLITCH,
                    cameraZoom = 1.04f,
                ),
                StoryBeat.Dialogue(
                    id = "ending_reaction",
                    speaker = cast.name,
                    text = cast.ending,
                    accent = cast.accent,
                    transition = StoryTransition.FADE,
                ),
                StoryBeat.MemoryUnlock(
                    id = "memory_unlock",
                    title = "Signal Fragment 00",
                    description = "A voice behind the screen called out through a damaged halo.",
                    accent = StoryAccent.CELESTIAL,
                ),
                StoryBeat.End("signal_end", 700L),
            ),
        )
    }

    private data class CastFlavor(
        val name: String,
        val imageResId: Int,
        val spiritResId: Int,
        val accent: StoryAccent,
        val opening: String,
        val ending: String,
    )
}
