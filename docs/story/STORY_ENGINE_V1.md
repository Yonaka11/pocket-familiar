# Pocket Familiar Story Engine V1.1

Pocket Familiar tells its story as a kinetic motion-comic that interrupts normal familiar play instead of opening a traditional visual-novel screen. The phone interface is part of the fiction: panels tilt, static tears across the UI, speech bubbles behave like manga lettering, and shadows can exist *behind* the interface.

## Core loop
1. Familiar behaves normally on the phone.
2. Something unusual causes a character reaction.
3. A short full-screen kinetic-comic episode begins.
4. The player taps through panels and performs a tactile interaction.
5. The episode awards a persistent Memory Fragment.
6. Normal familiar play returns, but the world may not look completely safe anymore.

## Engine pieces
- `StoryEpisode` — metadata, focus attendant, memory reward, ordered beats.
- `StoryBeat.Panel` — image panel with camera zoom and comic treatment.
- `StoryBeat.Dialogue` — manga-style speech or thought bubble.
- `StoryBeat.InkShadow` — procedural halftone/ink silhouette used for incomplete Seraphi and unknown presences.
- `StoryBeat.Flash` — short automatic impact cut.
- `StoryBeat.Interaction` — tactile beat such as clearing static, holding a signal, or tracing a glyph.
- `StoryBeat.MemoryUnlock` — persistent narrative reward.
- `StoryPlayer` — full-screen Compose renderer.
- `StoryProgressRepository` — DataStore persistence for episodes and Memory Fragments.

## Crash-safety rule
Story playback must not depend on the overlay service being alive.

Opening a story no longer starts/restarts `PetOverlayService`. Story images are presentation, not infrastructure: if a drawable fails to decode, the player displays an ink/static fallback instead of throwing. Overlay reactions are requested only after playback and only when the live selected familiar matches the episode focus.

This is deliberate because the previous build could enter the overlay/sprite path while launching the story, making a bad runtime art resource capable of taking the narrative screen down with it.

## Visual languages
### Emi
Electric yellow, cobalt, black, hard diagonal cuts, halftone shadows, glitch offsets, quick zooms, UI corruption. Her story scenes feel as though the phone itself is malfunctioning.

### Kaelani
Teal, bronze, cream, petal reveals, soft circular movement, botanical silhouettes and drifting foreground layers.

### Mira
Burgundy, muted teal, antique brass, page turns, handwritten notes, diagrams, ink and arcane marks. Mira remains a redhead.

### Seraphi
Early appearances are deliberately incomplete: damaged halo, violet-white signal fragments, cropped features, halftone shadows, broken silhouettes. Her presentation becomes cleaner as restoration progresses.

### Unknown presence
The second shadow language is intentionally distinct from Seraphi. It is heavier, blacker, more angular and less luminous. Episode 0 ends by implying that the thing still behind the interface is not necessarily the person who said `Find me.`

## Episode 0 — The Signal
Episode 0 follows the supplied 18-panel storyboard and is canonically Emi's perspective.

1. Pocket Familiar overlay active. Emi wanders at the edge of the screen.
2. Emi senses something: `...Wait.`
3. Close-up warning: `Don't touch that.`
4. Electric energy crawls *behind* the interface.
5. Emi: `Did you see that?`
6. A fragmented Seraphi presence appears in static.
7. Emi recoils: `No...`
8. Seraphi's outline forms. Her halo is broken.
9. Unknown voice from Seraphi's signal: `Find me.`
10. Player drags across the static tear to stabilize it.
11. Emi reaches toward the rupture: `Hold on!`
12. A second, threatening presence intrudes: `...who are you?`
13. Static clears; Seraphi breaks apart into fragments.
14. Emi drops down, stunned: `She was here...`
15. The normal interface returns, with faint residue.
16. `Signal Fragment 00` recovered.
17. Emi quietly asks: `Did you hear that too?`
18. The looming shadow remains behind the restored interface. The hook lingers.

First completion awards **25 Bond XP + 10 Charms to Emi**. Replays do not duplicate rewards.

## Storyboard asset pipeline
The three supplied Episode 0 storyboard pages can be converted into 18 runtime WebP panels by `tools/build_emk_runtime_assets.py` along with the EMK animation strips.

Expected source files:

```text
art_source/episode0_storyboard_page1.png
art_source/episode0_storyboard_page2.png
art_source/episode0_storyboard_page3.png
```

Generated destination:

```text
app/src/main/res/drawable-nodpi/story_ep0_01.webp
...
app/src/main/res/drawable-nodpi/story_ep0_18.webp
```

Until those final binary panels are bundled, Episode 0 uses stable existing character artwork plus procedural comic-ink Seraphi/unknown panels, preserving the same story rhythm without risking a decode crash.

## Planned Memory I arcs
- Emi — **Corrupted Signal**: damaged data and a pattern she recognizes but cannot place.
- Kaelani — **The Flower That Shouldn't Exist**: an impossible bloom carries a memory of a lost place.
- Mira — **The Page She Never Wrote**: a page appears in handwriting identical to hers, describing an event she cannot remember.

Eventually the same historical incident will be shown from several perspectives with conflicting details. The player reconstructs the truth instead of simply unlocking exposition.

## Design rules
- Choices and preferences may change dialogue, perspective and rewards, but should not hard-lock the main story.
- High-quality illustrations plus kinetic camera/comic treatment are preferable to low-quality animation spam.
- Character runtime movement and story cinematics are separate layers: story art may be more detailed than overlay sprites.
- Story data should remain authorable without creating a bespoke Compose screen for every episode.
- Broken or absent story art must degrade gracefully, never crash the app.
