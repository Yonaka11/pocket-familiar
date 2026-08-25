# Pocket Familiar Story Engine V1

Pocket Familiar tells its story as a kinetic motion-comic that breaks into normal familiar play instead of opening a traditional visual-novel dialogue screen.

## Core loop
1. Familiar behaves normally on the phone.
2. Something unusual causes a character reaction.
3. A short full-screen kinetic-manga episode begins.
4. The player taps through panels and performs a small interaction.
5. The episode awards a persistent Memory Fragment.
6. The familiar returns to normal play with a reaction state.

Story episodes are designed to feel like the phone itself is part of the world.

## Engine pieces
- `StoryEpisode` — episode metadata, memory reward, ordered beats.
- `StoryBeat.Panel` — illustrated panel with crop/zoom/transition language.
- `StoryBeat.Dialogue` — character dialogue without a conventional VN frame.
- `StoryBeat.Flash` — short automatic color/impact cut.
- `StoryBeat.Interaction` — tactile beat such as clearing static, holding a signal, or tracing a glyph.
- `StoryBeat.MemoryUnlock` — persistent narrative reward.
- `StoryPlayer` — full-screen Compose renderer for the timeline.
- `StoryProgressRepository` — DataStore persistence for completed episodes and recovered memory fragments.

## Visual languages
### Emi
Electric yellow, cobalt, black, hard diagonal cuts, glitch offsets, quick zooms, UI corruption.

### Kaelani
Teal, bronze, cream, petal reveals, soft circular movement, drifting organic forms.

### Mira
Burgundy, muted teal, antique brass, page-like transitions, diagrams, ink and arcane marks. Mira remains a redhead.

### Seraphi
Early appearances are incomplete: black, white, silver, damaged celestial light, cropped wings/halo/eyes. Her presentation becomes cleaner and more complete as restoration progresses.

## Episode 0 — The Signal
The first episode adapts its opening and ending dialogue to Emi, Kaelani, or Mira.

Beat outline:
1. Signal flash.
2. Current attendant stops mid-motion.
3. Character-specific warning.
4. Light appears behind the interface.
5. The attendant asks whether the player saw it.
6. White flash.
7. Fragmentary Seraphi image with damaged halo.
8. Unknown voice: `Find me.`
9. Player drags across static to stabilize the signal.
10. Attendant reacts to the collapse.
11. Character-specific final line.
12. `Signal Fragment 00` is recovered.
13. Normal familiar play resumes with a SPECIAL reaction.

First completion awards 25 Bond XP and 10 Charms. Replays do not duplicate the reward.

## Planned Memory I arcs
- Emi — **Corrupted Signal**: damaged data and a pattern she recognizes but cannot place.
- Kaelani — **The Flower That Shouldn't Exist**: an impossible bloom carries a memory of a lost place.
- Mira — **The Page She Never Wrote**: a page appears in handwriting identical to hers, describing an event she cannot remember.

The same past event will later be shown through multiple attendants with contradictory details. The player reconstructs the truth by comparing recovered memories rather than simply unlocking an exposition log.

## Design rules
- Preferences and choices may change perspective, dialogue, and rewards, but should not hard-lock main story progression.
- Static high-quality illustrations are preferred over low-quality frame spam. Motion comes from camera movement, panel transitions, masks, particles, limited facial swaps, and selective frame animation.
- Story data should remain authorable without creating a bespoke Compose screen for each episode.
- A broken story art asset should degrade gracefully rather than crash the app.
