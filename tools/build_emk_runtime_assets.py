#!/usr/bin/env python3
"""Build per-action EMK runtime strips from individually authored transparent frames.

Presentation boards are reference art only. This exporter deliberately refuses to
crop them. Runtime frames live under art_source/runtime_frames and are assembled
into one horizontal PNG per action, matching EmkRuntimeV2's strip contract.
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art_source" / "runtime_frames"
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
CELL = 200
EDGE_MARGIN = 6

CHARACTERS = {
    "emi": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("recover", 4), ("sleep", 3)],
    "kaelani": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("happy", 4), ("sleep", 3)],
    "mira": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("happy", 4), ("sleep", 3)],
}


def save_png_atomic(image: Image.Image, destination: Path) -> None:
    """Fully verify a PNG before and after atomically publishing it."""
    temporary = destination.with_name(f".{destination.name}.{os.getpid()}.tmp.png")
    try:
        image.save(temporary, "PNG", optimize=True)
        with temporary.open("rb") as stream:
            os.fsync(stream.fileno())
        with Image.open(temporary) as decoded:
            decoded.load()
            if decoded.mode != "RGBA" or decoded.size != image.size:
                raise ValueError(f"Temporary strip verification failed: {temporary}")
        os.replace(temporary, destination)
        with Image.open(destination) as decoded:
            decoded.load()
            if decoded.mode != "RGBA" or decoded.size != image.size:
                raise ValueError(f"Published strip verification failed: {destination}")
    finally:
        temporary.unlink(missing_ok=True)


def load_cropped(path: Path) -> Image.Image:
    with Image.open(path) as source:
        source.load()
        image = source.convert("RGBA")
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"Frame has no visible pixels: {path}")
    return image.crop(bounds)


def action_strip(character: str, action: str, count: int) -> Path:
    folder = SOURCE / character / action
    paths = [folder / f"{i:02d}.png" for i in range(count)]
    missing = [p for p in paths if not p.exists()]
    if missing:
        raise FileNotFoundError("Missing clean frames:\n" + "\n".join(f"  - {p.relative_to(ROOT)}" for p in missing))

    source_frames = [load_cropped(path) for path in paths]
    max_size = CELL - EDGE_MARGIN * 2
    scale = min(
        max_size / max(frame.width for frame in source_frames),
        max_size / max(frame.height for frame in source_frames),
        1.0,
    )
    frames = [
        frame.resize(
            (max(1, round(frame.width * scale)), max(1, round(frame.height * scale))),
            Image.Resampling.NEAREST,
        )
        if scale < 1.0
        else frame
        for frame in source_frames
    ]

    strip = Image.new("RGBA", (CELL * count, CELL), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        x = index * CELL + (CELL - frame.width) // 2
        y = CELL - frame.height - EDGE_MARGIN
        strip.alpha_composite(frame, (x, y))

    OUTPUT.mkdir(parents=True, exist_ok=True)
    destination = OUTPUT / f"{character}_anim_{action}.png"
    save_png_atomic(strip, destination)
    return destination


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--character",
        choices=sorted(CHARACTERS),
        help="build one character pack; omit to build all three",
    )
    args = parser.parse_args()
    selected = [args.character] if args.character else list(CHARACTERS)

    try:
        # Resolve every source before writing so a missing frame cannot leave a
        # selected character with a half-replaced runtime pack.
        jobs = [
            (character, action, count)
            for character in selected
            for action, count in CHARACTERS[character]
        ]
        for character, action, count in jobs:
            paths = [SOURCE / character / action / f"{i:02d}.png" for i in range(count)]
            missing = [path for path in paths if not path.exists()]
            if missing:
                raise FileNotFoundError(
                    "Missing clean frames:\n"
                    + "\n".join(f"  - {path.relative_to(ROOT)}" for path in missing)
                )

        for character, action, count in jobs:
            path = action_strip(character, action, count)
            print(f"wrote {path.relative_to(ROOT)}")
    except (FileNotFoundError, OSError, ValueError) as exc:
        print(exc, file=sys.stderr)
        print("Do not fall back to slicing showcase boards. Author the missing frame instead.", file=sys.stderr)
        return 2

    print("EMK per-action runtime strip export complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
