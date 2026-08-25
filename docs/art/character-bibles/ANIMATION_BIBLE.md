# EMK Shinobu-Quality Pixel Animation Bible

These three showcase sheets are the visual source of truth for Pocket Familiar's core cast. Runtime art should match the same crisp, high-density anime-pixel finish as the Shinobu reference sheets while remaining readable at overlay scale.

## Shared attendant animation contract
- Row 1: idle / orientation / blink / alternate idle
- Row 2: walk cycle (4 frames)
- Row 3: run cycle (4 frames)
- Row 4: jump / land / hurt-recover / character special

Recommended frame timing: idle 360-480 ms, walk 140-170 ms, run 90-120 ms, airborne 100-140 ms, special 180-260 ms.

## Emi — Sparkborn Trickster
Palette: deep dark skin, black bob with electric-yellow underlayer, amber eyes, black + electric yellow + cobalt + chrome.

Attendant states from showcase:
- Idle / blink
- Walk
- Run
- Jump / land
- Spark Swipe
- Volt Step
- Overclock
- Thunder Crown
- Hurt
- Recover
- Happy / victory
- Sleep / rest
- Get up
- Evolve / summon

Familiar form: Voltix, a compact cobalt-gold spark spirit.
- Idle breathing / ear flick
- Walk prowl
- Run / dash with electric trail
- Sleep curl
- Happy bounce
- Summon / return portal

Behavior: fastest and most playful of EMK; prefers runs, boops, double taps, catches, juggling, music, and trick throws.

## Kaelani — Bloom Weaver
Palette: warm golden-brown skin, dark flowing curls, teal + bronze + earthy gold + cream.

Attendant states from showcase:
- Idle / blink
- Front / back standing
- Walk
- Run
- Jump / land
- Petal Lash
- Bloom Burst
- Verdant Hymn
- Garden Sanctuary
- Hurt
- Knocked down
- Get up
- Victory / happy
- Sleep / rest
- Evolve / summon

Familiar form: Petalspirit, a floral spirit with teal-gold petal fins.
- Idle bloom pulse
- Walk glide
- Run petal streak
- Sleep / folded bloom
- Cheer / blossom burst

Behavior: graceful and composed; favors walking, grooming, petting, music, soft interactions, and gentle movement.

## Mira — Cozy Scholar
Mira is locked as a redhead. Palette: auburn-red hair, fair freckled skin, muted teal + charcoal + burgundy + antique brass.

Attendant states from showcase:
- Idle / blink
- Front / back
- Walk
- Run
- Jump / land
- Book Swing
- Arcane Glyph
- Sage's Focus
- Hurt / recover
- Happy / victory
- Sleep / rest
- Evolve / bond summon

Familiar form: Scribble, a tiny dream-scholar spirit.
- Idle reading / glasses adjustment
- Walk hop
- Run page-flutter dash
- Sleep on book stack
- Special reaction / arcane burst

Behavior: slowest and sleepiest of EMK; favors reading, food, sleep, petting, quiet idles, and dislikes rough juggling.

## Manual forms
Attendant and Familiar are player-selected forms. Form switching is manual and must never depend on time of day.

## Character Lab / devtools
The in-app Character Lab should remain available on development builds and expose:
- Emi / Kaelani / Mira selector
- Attendant / Familiar selector
- Idle, Walk, Run, Jump, Fall, Land, Held, Sleep, Happy, Hurt, Recover, Groom, Eat, Special
- Sprite size
- Animation speed
- Physics toggle / reset

## Runtime rules
1. A broken or undecodable atlas must never crash the overlay.
2. Fall back to preview/spirit art with procedural motion when an atlas cannot decode.
3. Pixel atlases must render without bitmap filtering.
4. Keep one character's art isolated from the others; no design bleed.
5. Production atlas exports must use transparent backgrounds, equal-size cells, and no labels or decorative panels.
