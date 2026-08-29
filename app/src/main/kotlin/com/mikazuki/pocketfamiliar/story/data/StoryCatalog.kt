package com.mikazuki.pocketfamiliar.story.data

import com.mikazuki.pocketfamiliar.R
import com.mikazuki.pocketfamiliar.story.model.StoryAccent
import com.mikazuki.pocketfamiliar.story.model.StoryBeat
import com.mikazuki.pocketfamiliar.story.model.StoryEpisode
import com.mikazuki.pocketfamiliar.story.model.StoryInteractionType
import com.mikazuki.pocketfamiliar.story.model.StoryPanelTreatment
import com.mikazuki.pocketfamiliar.story.model.StoryTransition

/**
 * Canon story timeline.
 *
 * Episode 0 follows the supplied 18-panel storyboard exactly in narrative order.
 * The current build renders those shots with reusable Compose comic treatments so
 * the story remains crash-safe even before final panel illustrations are bundled.
 */
object StoryCatalog {

    const val SIGNAL_EPISODE_ID = "episode_00_the_signal"
    const val SIGNAL_MEMORY_ID = "memory_signal_fragment_00"

    fun signalEpisode(selectedFamiliarId: String): StoryEpisode {
        val returningFromAnotherFamiliar = selectedFamiliarId !in setOf("emi", "default")
        val subtitle = if (returningFromAnotherFamiliar) {
            "Recovered perspective: Emi · something is moving behind the interface."
        } else {
            "Something is moving behind the interface."
        }

        return StoryEpisode(
            id = SIGNAL_EPISODE_ID,
            title = "Episode 0 · The Signal",
            subtitle = subtitle,
            memoryRewardId = SIGNAL_MEMORY_ID,
            memoryRewardTitle = "Signal Fragment 00",
            focusFamiliarId = "emi",
            beats = listOf(
                // Storyboard 01 — Pocket Familiar overlay active.
                StoryBeat.Panel(
                    id = "sb01_overlay_active",
                    imageResId = R.drawable.emi_avatar,
                    caption = "Pocket Familiar is active. Emi wanders at the edge of the screen.",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.FADE,
                    cameraZoom = 1.02f,
                    treatment = StoryPanelTreatment.COMIC_INK,
                ),
                // Storyboard 02 — Emi senses something.
                StoryBeat.Dialogue(
                    id = "sb02_wait",
                    speaker = "Emi",
                    text = "...Wait.",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.SLIDE_UP,
                ),
                // Storyboard 03 — close-up warning.
                StoryBeat.Panel(
                    id = "sb03_dont_touch",
                    imageResId = R.drawable.emi_avatar,
                    caption = "Don't touch that.",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.GLITCH,
                    cameraZoom = 1.24f,
                    treatment = StoryPanelTreatment.GLITCHED,
                ),
                // Storyboard 04 — energy crawls behind the UI.
                StoryBeat.Flash("sb04_interface_tear", StoryAccent.ELECTRIC, 115L),
                // Storyboard 05.
                StoryBeat.Dialogue(
                    id = "sb05_did_you_see",
                    speaker = "Emi",
                    text = "Did you see that?",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.SLIDE_LEFT,
                ),
                // Storyboard 06 — first Seraphi intrusion.
                StoryBeat.InkShadow(
                    id = "sb06_seraphi_static",
                    caption = "A presence resolves inside the static.",
                    accent = StoryAccent.CELESTIAL,
                    transition = StoryTransition.GLITCH,
                    halo = true,
                ),
                // Storyboard 07.
                StoryBeat.Dialogue(
                    id = "sb07_no",
                    speaker = "Emi",
                    text = "No...",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.GLITCH,
                ),
                // Storyboard 08 — halo forms from the broken signal.
                StoryBeat.InkShadow(
                    id = "sb08_seraphi_forms",
                    caption = "The signal gathers into the outline of a woman. Her halo will not stay whole.",
                    accent = StoryAccent.CELESTIAL,
                    transition = StoryTransition.WHITE_FLASH,
                    halo = true,
                ),
                // Storyboard 09.
                StoryBeat.Dialogue(
                    id = "sb09_find_me",
                    speaker = "???",
                    text = "Find me.",
                    accent = StoryAccent.CELESTIAL,
                    transition = StoryTransition.CUT,
                ),
                // Storyboard 10 — tactile clearing interaction.
                StoryBeat.Interaction(
                    id = "sb10_clear_static",
                    type = StoryInteractionType.CLEAR_STATIC,
                    prompt = "Drag across the tear. Clear the static before the signal collapses.",
                    completionText = "The interference splits open. Something underneath notices you.",
                    accent = StoryAccent.CELESTIAL,
                ),
                // Storyboard 11.
                StoryBeat.Dialogue(
                    id = "sb11_hold_on",
                    speaker = "Emi",
                    text = "Hold on!",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.GLITCH,
                ),
                // Storyboard 12 — the other presence.
                StoryBeat.InkShadow(
                    id = "sb12_unknown_presence",
                    caption = "...who are you?",
                    accent = StoryAccent.UNKNOWN,
                    transition = StoryTransition.CUT,
                    looming = true,
                ),
                // Storyboard 13 — static clears; Seraphi remains only as fragments.
                StoryBeat.InkShadow(
                    id = "sb13_seraphi_fades",
                    caption = "The static clears. Seraphi breaks apart into violet fragments.",
                    accent = StoryAccent.CELESTIAL,
                    transition = StoryTransition.WHITE_FLASH,
                    halo = true,
                ),
                // Storyboard 14.
                StoryBeat.Dialogue(
                    id = "sb14_she_was_here",
                    speaker = "Emi",
                    text = "She was here...",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.FADE,
                ),
                // Storyboard 15 — normal UI returns, residue remains.
                StoryBeat.Panel(
                    id = "sb15_interface_returns",
                    imageResId = R.drawable.emi_avatar,
                    caption = "Everything looks normal again. Almost.",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.SLIDE_UP,
                    treatment = StoryPanelTreatment.COMIC_INK,
                ),
                // Storyboard 16.
                StoryBeat.MemoryUnlock(
                    id = "sb16_memory_unlock",
                    title = "Signal Fragment 00",
                    description = "A voice behind the screen called through a broken halo.",
                    accent = StoryAccent.ELECTRIC,
                ),
                // Storyboard 17.
                StoryBeat.Dialogue(
                    id = "sb17_hear_that",
                    speaker = "Emi",
                    text = "Did you hear that too?",
                    accent = StoryAccent.ELECTRIC,
                    transition = StoryTransition.FADE,
                ),
                // Storyboard 18 — hook. The shadow was not Seraphi.
                StoryBeat.InkShadow(
                    id = "sb18_hook",
                    caption = "The quiet doesn't last.",
                    accent = StoryAccent.UNKNOWN,
                    transition = StoryTransition.GLITCH,
                    looming = true,
                ),
                StoryBeat.End("signal_end", 700L),
            ),
        )
    }
}
