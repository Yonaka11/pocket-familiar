#!/usr/bin/env python3
"""Assemble clean EMK Runtime V2 atlases from individually authored frames.

This intentionally DOES NOT crop frames from the presentation/character boards.
Those boards are reference art only. Runtime animation frames must be exported as
standalone transparent PNGs first so hair, limbs, effects, and neighboring poses
cannot be clipped or mirrored by poster slicing.

Expected input layout:

art_source/runtime_frames/
  emi/
    idle/00.png ... 03.png
    walk/00.png ... 03.png
    run/00.png ... 03.png
    jump_land/00.png ... 03.png
    special/00.png ... 03.png
    recover/00.png ... 02.png
    sleep/00.png ... 01.png
  kaelani/
    ... same, but reaction folder is happy/
  mira/
    ... same, but reaction folder is happy/

Outputs:
  app/src/main/res/drawable-nodpi/emi_runtime_v2_atlas.png
  app/src/main/res/drawable-nodpi/kaelani_runtime_v2_atlas.png
  app/src/main/res/drawable-nodpi/mira_runtime_v2_atlas.png

Atlas contract: 4 columns x 7 rows, 256x256 cells.
Rows are idle, walk, run, jump/land, special, reaction, sleep.
Short rows repeat their last valid frame only as padding; runtime never addresses
those padding cells.
"""

from __future__ import annotations

from pathlib import Path
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art_source" / "runtime_frames"
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

CELL = 256
COLS = 4
ROWS = 7

CHARACTERS = {
    "emi": [
        ("idle", 4),
        ("walk", 4),
        ("run", 4),
        ("jump_land", 4),
        ("special", 4),
        ("recover", 3),
        ("sleep", 2),
    ],
    "kaelani": [
        ("idle", 4),
        ("walk", 4),
        ("run", 4),
        ("jump_land", 4),
        ("special", 4),
        ("happy", 3),
        ("sleep", 2),
    ],
    "mira": [
        ("idle", 4),
        ("walk", 4),
        ("run", 4),
        ("jump_land", 4),
        ("special", 4),
        ("happy", 3),
        ("sleep", 2),
    ],
}


def alpha_bounds(image: Image.Image) -> tuple[int, int, int, int] | None:
    return image.getchannel("A").getbbox()


def normalize_frame(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    bounds = alpha_bounds(image)
    if bounds is None:
        raise ValueError(f"Frame has no visible pixels: {path}")
    image = image.crop(bounds)

    max_size = CELL - 16
    scale = min(max_size / image.width, max_size / image.height, 1.0)
    if scale < 1.0:
        image = image.resize(
            (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
            Image.Resampling.NEAREST,
        )
    return image


def load_action_frames(character: str, action: str, count: int) -> list[Image.Image]:
    folder = SOURCE / character / action
    paths = [folder / f"{index:02d}.png" for index in range(count)]
    missing = [path for path in paths if not path.exists()]
    if missing:
        names = "\n".join(f"  - {path.relative_to(ROOT)}" for path in missing)
        raise FileNotFoundError(f"Missing clean runtime frames:\n{names}")
    return [normalize_frame(path) for path in paths]


def build_character(character: str) -> Path:
    atlas = Image.new("RGBA", (COLS * CELL, ROWS * CELL), (0, 0, 0, 0))

    for row, (action, count) in enumerate(CHARACTERS[character]):
        frames = load_action_frames(character, action, count)
        padded = frames + [frames[-1]] * (COLS - len(frames))

        for column, frame in enumerate(padded[:COLS]):
            x = column * CELL + (CELL - frame.width) // 2
            # Bottom-align so feet share a stable baseline between frames.
            y = row * CELL + CELL - frame.height - 6
            atlas.alpha_composite(frame, (x, y))

    OUTPUT.mkdir(parents=True, exist_ok=True)
    destination = OUTPUT / f"{character}_runtime_v2_atlas.png"
    atlas.save(destination, "PNG", optimize=True)
    return destination


def main() -> int:
    try:
        for character in CHARACTERS:
            path = build_character(character)
            print(f"wrote {path.relative_to(ROOT)}")
    except (FileNotFoundError, ValueError) as error:
        print(error, file=sys.stderr)
        print(
            "\nDo not fall back to cropping the showcase boards. Export the missing "
            "pose as a standalone transparent frame instead.",
            file=sys.stderr,
        )
        return 2

    print("\nEMK Runtime V2 clean-atlas export complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
