# EMK Runtime V2 — Production Animation Contract

The next Pocket Familiar runtime art pass moves Emi, Kaelani, and Mira away from the earlier super-deformed / single-pose look and toward the newer locked direction: serious-but-cute pixel characters with readable anatomy, richer clothing, stronger silhouettes, and genuine multi-frame motion.

The supplied character sheets are the visual source of truth. Mira remains a redhead.

## Runtime format

Each action is exported as its own **horizontal transparent sprite strip** instead of forcing every behavior into one tiny 4×4 atlas.

- Cell size: **200 × 200 px**
- Equal-width cells
- Transparent background
- Nearest-neighbor rendering
- No labels, borders, UI, presentation panels, or decorative text inside runtime files
- Character anchor stays near bottom-center of every cell
- Left/right movement should be handled by runtime mirroring where practical

This layout makes it possible to give locomotion and reactions enough transition frames instead of sliding a single drawing around the screen.

## Emi

Visual language: black, electric yellow, cobalt, chrome; sharp tech-royal silhouette; quick energetic movement.

Expected files:

| File | Frames | Runtime use |
| --- | ---: | --- |
| `emi_anim_idle.webp` | 4 | idle breathing, glance, blink |
| `emi_anim_walk.webp` | 4 | full walk cycle |
| `emi_anim_run.webp` | 4 | full run cycle |
| `emi_anim_jump_land.webp` | 4 | anticipation → airborne → landing → recovery |
| `emi_anim_special.webp` | 4 | Glitch Sovereign / electric special |
| `emi_anim_recover.webp` | 3 | hurt → brace → recover |
| `emi_anim_sleep.webp` | 2 | settle → breathing/rest |

Timing target: idle 240–360 ms/frame, walk 120–165 ms/frame, run 80–115 ms/frame, special 110–180 ms/frame.

## Kaelani

Visual language: warm bronze skin, dark flowing curls, teal/bronze/earthy gold, bloom magic; graceful rather than bouncy.

Expected files:

| File | Frames | Runtime use |
| --- | ---: | --- |
| `kaelani_anim_idle.webp` | 4 | breathing, hair settling, blink |
| `kaelani_anim_walk.webp` | 4 | flowing walk cycle |
| `kaelani_anim_run.webp` | 4 | controlled run cycle |
| `kaelani_anim_jump_land.webp` | 4 | lift → airborne → land → settle |
| `kaelani_anim_special.webp` | 4 | Blooming Grace / lotus sequence |
| `kaelani_anim_happy.webp` | 3 | gentle happy / affectionate reaction |
| `kaelani_anim_sleep.webp` | 2 | seated/rest → sleep |

Timing target: idle 280–400 ms/frame, walk 135–175 ms/frame, run 95–125 ms/frame, special 150–220 ms/frame.

## Mira

Visual language: **red hair**, glasses, burgundy/charcoal/muted teal/antique brass, scholar/arcanist details; compact, thoughtful movement.

Expected files:

| File | Frames | Runtime use |
| --- | ---: | --- |
| `mira_anim_idle.webp` | 4 | breathing, glance, blink |
| `mira_anim_walk.webp` | 4 | careful walk cycle |
| `mira_anim_run.webp` | 4 | hurried scholar run |
| `mira_anim_jump_land.webp` | 4 | crouch → jump → land → recover |
| `mira_anim_special.webp` | 4 | Arcanist's Page / book-and-glyph sequence |
| `mira_anim_happy.webp` | 3 | curious/thinking/happy reaction |
| `mira_anim_sleep.webp` | 2 | book-stack rest → blanket sleep |

Timing target: idle 300–420 ms/frame, walk 145–185 ms/frame, run 100–135 ms/frame, special 150–230 ms/frame.

## State mapping

When the exported resources are present, `PetRegistry` should map states approximately as follows:

- `Idle` → `*_anim_idle`
- `WalkLeft/WalkRight` and climbing → `*_anim_walk`
- `RunLeft/RunRight` and step celebration → `*_anim_run`
- `Jumping/Falling/Thrown/HardLanding/Recovering` → phase-appropriate frames from `*_anim_jump_land` / recover
- `Happy`, preferred touch, music, grooming → character reaction strip or special strip
- `Sleep/DeepSleep/Charging/LowBattery` → `*_anim_sleep`
- Character Lab `SPECIAL` → `*_anim_special`

The runtime should not apply procedural bobbing to a character while a genuine multi-frame strip is playing. Procedural motion remains a fallback for one-image Familiar-form prototypes only.

## Reproducible exporter

`tools/build_emk_runtime_assets.py` contains the crop map for the supplied character sheets and exports the runtime strips deterministically.

Place the locked source files at:

```text
art_source/emi_character_sheet.png
art_source/kaelani_character_sheet.png
art_source/mira_character_sheet.png
art_source/episode0_storyboard_page1.png
art_source/episode0_storyboard_page2.png
art_source/episode0_storyboard_page3.png
```

Then run:

```bash
python -m pip install pillow numpy
python tools/build_emk_runtime_assets.py
```

The generated binary art is intentionally separate from the showcase/reference sheets. The showcase art remains useful as a character bible, but presentation sheets must never be sliced directly by the Android runtime.
