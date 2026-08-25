# EMK Shinobu-Quality Animation Map

These character boards are the visual source of truth for Pocket Familiar's EMK pixel art. The target is the same crisp anime-pixel readability, silhouette control, facial expression quality, and effect polish used by the Shinobu character sheets already in the project.

## Shared runtime atlas contract

Attendant forms use a 4×4 transparent runtime atlas. Keep every cell the same size and keep the character's feet/baseline stable between frames.

- Row 1, frames 0–3: idle/orientation. 0 idle A, 1 blink/idle B, 2 side/profile, 3 back/reference.
- Row 2, frames 4–7: four-frame walk cycle.
- Row 3, frames 8–11: four-frame run cycle.
- Row 4, frames 12–15: reaction/special states. 12 happy/victory, 13 jump/hurt/airborne reaction, 14 sleep/rest, 15 signature special.

The current Kotlin mapping intentionally shares a few row-4 frames between compatible states so the app can ship a strong 16-frame core before expanding into larger atlases.

## Emi — Sparkborn Trickster

Palette: black, electric yellow/gold, cobalt/electric blue, chrome/white highlights. Deep dark skin, amber-gold eyes, black bob with neon-yellow underlayer.

Attendant targets:
- Idle/blink: confident tech-royal stance, subtle energy flicker.
- Walk: quick light steps with hair/coat follow-through.
- Run: aggressive forward lean with spark trail.
- Jump/land: compact launch and electric impact.
- Happy/victory: bright grin, crown/spark reaction.
- Signature: Thunder Crown / Overclock-style electric ring and circuit effects.

Familiar identity: **Voltix**, Sparkborn Spirit. Long-eared dark cobalt/gold electric spirit. Familiar animation set should include idle breathing, walk, run/zoomies, sleep curl, happy spark reaction, and summon/return.

## Kaelani — Bloom-Weaver Attendant

Palette: teal, bronze, earthy gold, cream, warm floral highlights. Warm golden-brown skin, hazel-gold eyes, long dark curls.

Attendant targets:
- Idle/blink: calm centered posture with soft curl and fabric motion.
- Walk: elegant measured stride.
- Run: flowing forward motion without losing grace.
- Jump/land: soft bloom/petal landing.
- Happy/victory: warm smile with small floral glow.
- Signature: Garden Sanctuary / Verdant Hymn-style petals, teal-gold rings, leaves and moon-bloom effects.

Familiar identity: **Petalspirit**, Floral Spirit. Compact lotus-like guardian with a dark face core and cream/teal petals. Familiar animation set should include idle petal breathing, gliding walk, faster bloom-run, folded-petal sleep, cheer/bloom reaction, and summon/return.

## Mira — Cozy Scholar

Mira is canonically a **redhead**. Palette: copper/auburn hair, muted teal, charcoal, burgundy, antique brass, warm parchment highlights. Glasses remain a key readability feature.

Attendant targets:
- Idle/blink: quiet bookish stance with glasses and hair readable at small scale.
- Walk: contained, slightly careful movement.
- Run: hurried scholar energy with hair/clothing follow-through.
- Jump/land: surprised but controlled reaction.
- Happy/victory: shy delighted expression.
- Sleep/rest: curled beside books/notes when space permits.
- Signature: Sage's Focus / Arcane Glyph-style books, runes, stars and teal-burgundy magic.

Familiar identity: **Scribble**, Dreamwatch Scholar. Small round scholar spirit with glasses/hat/book motifs. Familiar animation set should include idle hover/breathing, walk, run, sleep on/near a book, surprised/happy reaction, and summon/return.

## Character Lab / dev tools

The Character Lab is part of the production workflow and should not be removed when rebasing or syncing from Cursor. It must let the developer force Idle, Walk, Run, Jump, Fall, Held, Sleep, Happy, Special, Groom, and Eat so each atlas can be checked instantly on-device.

Form selection is manual. Emi, Kaelani, and Mira must never switch between Attendant and Familiar according to device time.

## Art acceptance checklist

A production atlas is accepted only when:
- transparent background, no labels or presentation panels in runtime assets;
- equal cells with no sprite bleeding across cell boundaries;
- consistent character scale and ground baseline;
- crisp outlines with no filtering blur;
- readable eyes, hair silhouette, hands/feet and signature accessories at overlay size;
- walk/run cycles do not visibly resize the character;
- effects stay inside the intended cell unless deliberately cropped by design;
- Mira remains visibly red-haired in every frame;
- the atlas decodes successfully on Android and failure still falls back without crashing the overlay.
