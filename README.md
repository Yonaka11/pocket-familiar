# Pocket Familiar

A free, ad-free Android companion app where animated familiars live on top of other apps as persistent floating characters.

Pocket Familiar combines overlay pets, tactile physics, touch play, progression, gifts, activity rewards, character preferences, and a light story about restoring the celestial boss **Seraphi Astrea**.

**Package:** `com.mikazuki.pocketfamiliar`  
**Min SDK:** 26 (Android 8.0)  
**Target SDK:** 35 (Android 15)

---

## Current gameplay direction

- Persistent familiar overlay with walking, running, climbing, sleeping, falling, grabbing, throwing, bouncing, and recovery
- Tactile interactions including taps, boops, petting, tickling, catches, juggling, airtime, and trick throws
- Bond XP, Play XP, per-familiar saved progression, achievements, steps, Charms, and gifts
- Character preferences that provide bonus flavor/rewards without ever blocking progression
- Cute autonomous behaviors including eating, grooming, happy reactions, sleep, and deep sleep
- Atlas-backed multi-character animation system
- Battery/activity reaction hooks for charging, low battery, music, and walking

---

## Canon story

**Seraphi Astrea** is a Seraphim, Celestial Keeper, and boss of Emi, Kaelani, and Mira (EMK). She has lost her human form and begins the story sealed in a small celestial spirit-familiar body.

EMK help the player restore Seraphi by building Bond, Play mastery, achievements, memories, and activity progress.

### Day / night forms

- **Emi, Kaelani, Mira by day:** humanoid/chibi familiar forms
- **Emi, Kaelani, Mira by night:** anime spirit-animal forms
- Their personality remains consistent across both forms

### Seraphi progression

1. **Base Spirit Form** — initial mascot/boss state
2. **Ascended Seraphim Spirit** — six wings fully manifest after meaningful EMK progress
3. **Restored Human Form** — major story unlock earned by progressing all three attendants

See [`docs/CHARACTER_BIBLE.md`](docs/CHARACTER_BIBLE.md) for the locked character/lore spec.

---

## Build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17 and Android SDK API 35.

---

## Install and run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mikazuki.pocketfamiliar/.MainActivity
```

1. Grant overlay permission
2. Start the familiar
3. Open another app and interact with the overlay
4. Drag, flick, catch, pet, boop, and experiment with reactions
5. Progress is persisted locally per familiar

---

## Architecture

The runtime is split into familiar definitions, behavior/state, physics, overlay rendering, progression/rewards, and persistent repositories. Canon/story definitions are deliberately kept separate from the currently selectable runtime roster so unfinished characters and future forms can be designed without forcing incomplete assets into production.

Key areas:

```text
app/src/main/kotlin/com/mikazuki/pocketfamiliar/
├── model/        familiar definitions, preferences, progression, canon, story rules
├── data/         settings and familiar progression persistence
├── pet/          animation, behavior/state, and physics
├── overlay/      WindowManager overlay and sprite renderer
├── service/      foreground overlay service and gameplay integration
├── ui/           Compose controls, progression, gifts, settings
└── util/         battery and step monitoring
```

---

## Progression philosophy

Pocket Familiar should reward normal life rather than dictate it.

- All activities can progress every character
- Favorite activities only add modest bonuses and special reactions
- Non-preferred activities never reduce XP
- No punishment for missed streaks or inactivity
- Gifts, play, steps, music, discoveries, and memories all contribute to the relationship loop

---

## Roadmap

- [x] Multi-character sprite/atlas support
- [x] Grab and two-dimensional throw physics
- [x] Per-familiar physical profiles
- [x] Cute reaction animation states
- [x] Touch play and juggle/catch scoring foundation
- [x] Bond XP, Play XP, Charms, gifts, achievements, step rewards
- [x] Locked EMK + Seraphi Astrea canon/story foundation
- [ ] Day/night form switching
- [ ] EMK production day-form sprite packs
- [ ] EMK production night spirit-animal sprite packs
- [ ] Seraphi Base Spirit runtime character
- [ ] Seraphi Ascended Spirit unlock
- [ ] Memory/discovery system
- [ ] Seraphi Restored Human story unlock
- [ ] Music playback reactions
- [ ] Cloud backup/sync
- [ ] Original-character familiar creator

---

## License

MIT
