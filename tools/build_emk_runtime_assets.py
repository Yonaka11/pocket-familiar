#!/usr/bin/env python3
"""Build per-action EMK runtime strips from individually authored transparent frames.

Presentation boards are reference art only. This exporter deliberately refuses to
crop them. Runtime frames live under art_source/runtime_frames and are assembled
into one horizontal PNG per action, matching EmkRuntimeV2's strip contract.
"""

from __future__ import annotations

from pathlib import Path
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art_source" / "runtime_frames"
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
CELL = 80

CHARACTERS = {
    "emi": [("idle", 4), ("walk", 4), ("run", 4), ("jump_land", 4), ("special", 4), ("recover", 3), ("sleep", 2)],
    "kaelani": [("idle", 4), ("walk", 4), ("run", 4), ("jump_land", 4), ("special", 4), ("happy", 3), ("sleep", 2)],
    "mira": [("idle", 4), ("walk", 4), ("run", 4), ("jump_land", 4), ("special", 4), ("happy", 3), ("sleep", 2)],
}


def normalize(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGBA")
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError(f"Frame has no visible pixels: {path}")
    image = image.crop(bounds)

    max_size = CELL - 4
    scale = min(max_size / image.width, max_size / image.height, 1.0)
    if scale < 1.0:
        image = image.resize(
            (max(1, round(image.width * scale)), max(1, round(image.height * scale))),
            Image.Resampling.NEAREST,
        )
    return image


def action_strip(character: str, action: str, count: int) -> Path:
    folder = SOURCE / character / action
    paths = [folder / f"{i:02d}.png" for i in range(count)]
    missing = [p for p in paths if not p.exists()]
    if missing:
        raise FileNotFoundError("Missing clean frames:\n" + "\n".join(f"  - {p.relative_to(ROOT)}" for p in missing))

    strip = Image.new("RGBA", (CELL * count, CELL), (0, 0, 0, 0))
    for index, path in enumerate(paths):
        frame = normalize(path)
        x = index * CELL + (CELL - frame.width) // 2
        y = CELL - frame.height - 2
        strip.alpha_composite(frame, (x, y))

    OUTPUT.mkdir(parents=True, exist_ok=True)
    destination = OUTPUT / f"{character}_anim_{action}.png"
    strip.save(destination, "PNG", optimize=True)
    return destination


def main() -> int:
    try:
        for character, actions in CHARACTERS.items():
            for action, count in actions:
                path = action_strip(character, action, count)
                print(f"wrote {path.relative_to(ROOT)}")
    except (FileNotFoundError, ValueError) as exc:
        print(exc, file=sys.stderr)
        print("Do not fall back to slicing showcase boards. Author the missing frame instead.", file=sys.stderr)
        return 2

    print("EMK per-action runtime strip export complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
