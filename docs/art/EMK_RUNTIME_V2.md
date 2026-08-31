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
| `emi_anim_idle.png` | 4 | idle breathing, glance, blink |
| `emi_anim_walk.png` | 6 | full walk cycle |
| `emi_anim_run.png` | 6 | full run cycle |
| `emi_anim_jump_land.png` | 4 | anticipation → airborne → landing → recovery |
| `emi_anim_special.png` | 6 | Glitch Sovereign / electric special |
| `emi_anim_recover.png` | 4 | hurt → brace → recover |
| `emi_anim_sleep.png` | 3 | settle → breathing/rest |

Timing target: idle 240–360 ms/frame, walk 120–165 ms/frame, run 80–115 ms/frame, special 110–180 ms/frame.

## Kaelani

Visual language: warm bronze skin, dark flowing curls, teal/bronze/earthy gold, bloom magic; graceful rather than bouncy.

Expected files:

| File | Frames | Runtime use |
| --- | ---: | --- |
| `kaelani_anim_idle.png` | 4 | breathing, hair settling, blink |
| `kaelani_anim_walk.png` | 6 | flowing walk cycle |
| `kaelani_anim_run.png` | 6 | controlled run cycle |
| `kaelani_anim_jump_land.png` | 4 | lift → airborne → land → settle |
| `kaelani_anim_special.png` | 6 | Blooming Grace / lotus sequence |
| `kaelani_anim_happy.png` | 4 | gentle happy / affectionate reaction |
| `kaelani_anim_sleep.png` | 3 | seated/rest → sleep |

Timing target: idle 280–400 ms/frame, walk 135–175 ms/frame, run 95–125 ms/frame, special 150–220 ms/frame.

## Mira

Visual language: **red hair**, glasses, burgundy/charcoal/muted teal/antique brass, scholar/arcanist details; compact, thoughtful movement.

Expected files:

| File | Frames | Runtime use |
| --- | ---: | --- |
| `mira_anim_idle.png` | 4 | breathing, glance, blink |
| `mira_anim_walk.png` | 6 | careful walk cycle |
| `mira_anim_run.png` | 6 | hurried scholar run |
| `mira_anim_jump_land.png` | 4 | crouch → jump → land → recover |
| `mira_anim_special.png` | 6 | Arcanist's Page / book-and-glyph sequence |
| `mira_anim_happy.png` | 4 | curious/thinking/happy reaction |
| `mira_anim_sleep.png` | 3 | book-stack rest → blanket sleep |

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

`tools/ingest_emk_action_sheets.py` splits approved transparent action sheets into individual frames. `tools/build_emk_runtime_assets.py` then normalizes those frames and exports the runtime strips deterministically.

The current approved production sheets arrived with a baked checkerboard. Their one-time recovery is implemented by `tools/extract_emk_checkerboard_frames.py`: it clears only spatially matched, edge-connected checker pixels, preserves retained RGB bytes exactly, and records the four individually reviewed geometric exclusions in `docs/art/EMK_BACKGROUND_EXTRACTION_AUDIT.json`. It does not recolor, rescale, redraw, or substitute character art. This recovery path is not a replacement for receiving true-alpha source sheets.

Place the approved transparent action sheets at:

```text
animation_work/generated/<character>/<action>_sheet.png
```

Then run:

```bash
python -m pip install pillow
python tools/ingest_emk_action_sheets.py
python tools/build_emk_runtime_assets.py
```

For the audited one-time checkerboard recovery, install `numpy`, `scipy`, and `pillow`, then run `tools/extract_emk_checkerboard_frames.py` before the strip builder. The builder verifies each PNG before and after publishing it via an atomic rename, preventing partially written runtime strips.

The generated binary art is intentionally separate from the showcase/reference sheets. The showcase art remains useful as a character bible, but presentation sheets must never be sliced directly by the Android runtime.
