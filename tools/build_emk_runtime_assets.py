#!/usr/bin/env python3
"""Build Pocket Familiar runtime art from the locked EMK reference sheets.

Expected source files (not committed by this script):
  art_source/emi_character_sheet.png
  art_source/kaelani_character_sheet.png
  art_source/mira_character_sheet.png
  art_source/episode0_storyboard_page1.png
  art_source/episode0_storyboard_page2.png
  art_source/episode0_storyboard_page3.png

Outputs:
  app/src/main/res/drawable-nodpi/{emi,kaelani,mira}_anim_*.webp
  app/src/main/res/drawable-nodpi/story_ep0_01.webp ... story_ep0_18.webp

Install dependencies:
  python -m pip install pillow numpy

The crop map is intentionally versioned with the app so the same visual source can
be re-exported deterministically instead of hand-cutting sprite files each time.
"""

from __future__ import annotations

from pathlib import Path
import sys

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art_source"
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
CELL = 200

CHARACTER_SHEETS = {
    "emi": SOURCE / "emi_character_sheet.png",
    "kaelani": SOURCE / "kaelani_character_sheet.png",
    "mira": SOURCE / "mira_character_sheet.png",
}

# centers, y1, y2, left bound, right bound
ANIMATION_CROPS = {
    "emi": {
        "idle": ([1090, 1180, 1275, 1370], 82, 205, 1045, 1435),
        "walk": ([65, 165, 265, 365], 265, 410, 15, 415),
        "run": ([520, 620, 720, 825], 265, 410, 465, 880),
        "jump_land": ([995, 1090, 1190, 1300], 265, 415, 940, 1375),
        "special": ([65, 205, 345, 500], 500, 680, 5, 580),
        "recover": ([670, 805, 910], 510, 680, 620, 965),
        "sleep": ([1040, 1220], 520, 680, 970, 1330),
    },
    "kaelani": {
        "idle": ([1090, 1185, 1280, 1370], 60, 195, 1040, 1435),
        "walk": ([70, 170, 275, 370], 230, 380, 10, 415),
        "run": ([520, 625, 730, 835], 230, 390, 465, 895),
        "jump_land": ([970, 1080, 1200, 1340], 220, 395, 910, 1410),
        "special": ([70, 235, 410, 560], 445, 620, 10, 635),
        "happy": ([750, 850, 950], 445, 615, 700, 1005),
        "sleep": ([1095, 1260], 440, 620, 1030, 1370),
    },
    "mira": {
        "idle": ([1140, 1220, 1300, 1380], 65, 195, 1090, 1435),
        "walk": ([65, 165, 265, 365], 280, 420, 10, 415),
        "run": ([485, 590, 695, 800], 280, 425, 430, 845),
        "jump_land": ([955, 1050, 1135, 1235], 280, 425, 905, 1285),
        "special": ([75, 235, 410, 580], 495, 655, 10, 660),
        "happy": ([770, 895, 1000], 490, 655, 710, 1045),
        "sleep": ([1135, 1300], 490, 655, 1060, 1405),
    },
}

STORY_PAGES = {
    1: SOURCE / "episode0_storyboard_page1.png",
    2: SOURCE / "episode0_storyboard_page2.png",
    3: SOURCE / "episode0_storyboard_page3.png",
}

# x1, y1, x2, y2. Panels are numbered in playback order.
STORY_PANEL_CROPS = {
    1: [
        (178, 14, 920, 266),
        (178, 279, 920, 524),
        (178, 535, 920, 790),
        (178, 800, 920, 1016),
        (178, 1027, 920, 1316),
        (178, 1327, 920, 1640),
    ],
    2: [
        (166, 12, 920, 280),
        (166, 291, 920, 553),
        (166, 563, 920, 786),
        (166, 795, 920, 1029),
        (166, 1038, 920, 1310),
        (166, 1319, 920, 1626),
    ],
    3: [
        (177, 12, 914, 273),
        (177, 283, 914, 570),
        (177, 578, 914, 797),
        (177, 805, 914, 988),
        (177, 997, 914, 1214),
        (177, 1222, 914, 1545),
    ],
}


def _bounds(centers: list[int], left: int, right: int) -> list[int]:
    return [left] + [int((a + b) / 2) for a, b in zip(centers, centers[1:])] + [right]


def _color_key(crop: Image.Image) -> Image.Image:
    """Remove the dark navy showcase background while preserving black line art."""
    rgb = np.array(crop.convert("RGB"), dtype=np.float32)
    corners = np.concatenate(
        [
            rgb[:6, :6].reshape(-1, 3),
            rgb[:6, -6:].reshape(-1, 3),
            rgb[-6:, :6].reshape(-1, 3),
            rgb[-6:, -6:].reshape(-1, 3),
        ]
    )
    background = np.median(corners, axis=0)
    distance = np.linalg.norm(rgb - background, axis=2)
    red, green, blue = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]

    alpha = np.clip((distance - 5.0) * 22.0, 0, 255).astype(np.uint8)
    # Keep genuine black outlines/shadows. The source background is navy, not true black.
    alpha[(red < 4) & (green < 4) & (blue < 5)] = 255
    alpha[distance > 18] = 255

    rgba = np.dstack([rgb.astype(np.uint8), alpha])
    return Image.fromarray(rgba, "RGBA")


def _trim_alpha(image: Image.Image, pad: int = 3) -> Image.Image:
    box = image.getchannel("A").getbbox()
    if not box:
        return image
    left, top, right, bottom = box
    return image.crop(
        (
            max(0, left - pad),
            max(0, top - pad),
            min(image.width, right + pad),
            min(image.height, bottom + pad),
        )
    )


def build_animation_strips() -> None:
    for character, source_path in CHARACTER_SHEETS.items():
        if not source_path.exists():
            raise FileNotFoundError(source_path)
        source = Image.open(source_path).convert("RGBA")

        for action, (centers, y1, y2, left, right) in ANIMATION_CROPS[character].items():
            boundaries = _bounds(centers, left, right)
            frames: list[Image.Image] = []

            for index in range(len(centers)):
                crop = source.crop((boundaries[index], y1, boundaries[index + 1], y2))
                frames.append(_trim_alpha(_color_key(crop)))

            strip = Image.new("RGBA", (CELL * len(frames), CELL), (0, 0, 0, 0))
            for index, frame in enumerate(frames):
                scale = min((CELL - 10) / frame.width, (CELL - 10) / frame.height, 1.0)
                if scale < 1.0:
                    frame = frame.resize(
                        (max(1, int(frame.width * scale)), max(1, int(frame.height * scale))),
                        Image.Resampling.NEAREST,
                    )
                x = index * CELL + (CELL - frame.width) // 2
                y = CELL - frame.height - 3
                strip.alpha_composite(frame, (x, y))

            destination = OUTPUT / f"{character}_anim_{action}.webp"
            strip.save(destination, "WEBP", quality=100, method=6, exact=True)
            print(f"wrote {destination.relative_to(ROOT)} ({len(frames)} frames)")


def build_story_panels() -> None:
    panel = 1
    for page_number in (1, 2, 3):
        source_path = STORY_PAGES[page_number]
        if not source_path.exists():
            raise FileNotFoundError(source_path)
        source = Image.open(source_path).convert("RGB")

        for crop_box in STORY_PANEL_CROPS[page_number]:
            image = source.crop(crop_box)
            destination = OUTPUT / f"story_ep0_{panel:02d}.webp"
            image.save(destination, "WEBP", quality=82, method=6)
            print(f"wrote {destination.relative_to(ROOT)}")
            panel += 1


def main() -> int:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    missing = [path for path in [*CHARACTER_SHEETS.values(), *STORY_PAGES.values()] if not path.exists()]
    if missing:
        print("Missing locked source art:", file=sys.stderr)
        for path in missing:
            print(f"  - {path.relative_to(ROOT)}", file=sys.stderr)
        print("\nCopy the six supplied reference images into art_source/ and rerun.", file=sys.stderr)
        return 2

    build_animation_strips()
    build_story_panels()
    print("\nRuntime V2 art export complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
