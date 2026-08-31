#!/usr/bin/env python3
"""Split approved transparent EMK action sheets into deterministic source frames."""

from __future__ import annotations

import argparse
from hashlib import sha256
import os
from pathlib import Path
import sys

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "art_source" / "runtime_frames"
CHARACTERS = {
    "emi": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("recover", 4), ("sleep", 3)],
    "kaelani": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("happy", 4), ("sleep", 3)],
    "mira": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("happy", 4), ("sleep", 3)],
}
GRIDS = {3: (3, 1), 4: (2, 2), 6: (3, 2)}
SOURCE_EDGE_MARGIN = 2


def save_png_atomic(image: Image.Image, destination: Path) -> None:
    temporary = destination.with_name(f".{destination.name}.{os.getpid()}.tmp.png")
    try:
        image.save(temporary, "PNG", optimize=True)
        with temporary.open("rb") as stream:
            os.fsync(stream.fileno())
        with Image.open(temporary) as decoded:
            decoded.load()
            if decoded.mode != "RGBA" or decoded.size != image.size:
                raise ValueError(f"Temporary frame verification failed: {temporary}")
        os.replace(temporary, destination)
        with Image.open(destination) as decoded:
            decoded.load()
            if decoded.mode != "RGBA" or decoded.size != image.size:
                raise ValueError(f"Published frame verification failed: {destination}")
    finally:
        temporary.unlink(missing_ok=True)


def default_sheet_root() -> Path:
    candidates = [
        ROOT / "animation_work" / "generated",
        ROOT.parent / "animation_work" / "generated",
    ]
    return next((path for path in candidates if path.exists()), candidates[0])


def split_sheet(path: Path, count: int) -> list[Image.Image]:
    with Image.open(path) as source:
        source.load()
        sheet = source.convert("RGBA")

    columns, rows = GRIDS[count]
    if sheet.width % columns or sheet.height % rows:
        raise ValueError(
            f"{path}: {sheet.width}x{sheet.height} is not evenly divisible "
            f"by its {columns}x{rows} grid"
        )
    cell_width = sheet.width // columns
    cell_height = sheet.height // rows
    frames: list[Image.Image] = []

    for index in range(count):
        column = index % columns
        row = index // columns
        cell = sheet.crop(
            (
                column * cell_width,
                row * cell_height,
                (column + 1) * cell_width,
                (row + 1) * cell_height,
            )
        )
        alpha = cell.getchannel("A")
        bounds = alpha.getbbox()
        if bounds is None:
            raise ValueError(f"{path}: frame {index:02d} is blank")
        left, top, right, bottom = bounds
        if (
            left < SOURCE_EDGE_MARGIN
            or top < SOURCE_EDGE_MARGIN
            or right > cell_width - SOURCE_EDGE_MARGIN
            or bottom > cell_height - SOURCE_EDGE_MARGIN
        ):
            raise ValueError(
                f"{path}: frame {index:02d} touches a grid edge and may be clipped; "
                f"alpha bounds={bounds}, cell={cell_width}x{cell_height}"
            )
        if alpha.getextrema() == (255, 255):
            raise ValueError(f"{path}: frame {index:02d} has no transparent background")
        frames.append(cell.crop(bounds))

    digests = [sha256(frame.tobytes()).hexdigest() for frame in frames]
    if len(set(digests)) != len(digests):
        raise ValueError(f"{path}: contains duplicate frames")
    return frames


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sheet-root", type=Path, default=default_sheet_root())
    parser.add_argument(
        "--character",
        choices=sorted(CHARACTERS),
        help="ingest one character; omit to ingest all three",
    )
    args = parser.parse_args()
    selected = [args.character] if args.character else list(CHARACTERS)

    try:
        staged: list[tuple[Path, list[Image.Image]]] = []
        for character in selected:
            for action, count in CHARACTERS[character]:
                sheet = args.sheet_root / character / f"{action}_sheet.png"
                if not sheet.exists():
                    raise FileNotFoundError(f"Missing approved action sheet: {sheet}")
                staged.append((OUTPUT / character / action, split_sheet(sheet, count)))

        for folder, frames in staged:
            folder.mkdir(parents=True, exist_ok=True)
            for index, frame in enumerate(frames):
                destination = folder / f"{index:02d}.png"
                save_png_atomic(frame, destination)
                print(f"wrote {destination.relative_to(ROOT)}")
    except (FileNotFoundError, OSError, ValueError) as exc:
        print(exc, file=sys.stderr)
        return 2

    print("EMK action-sheet ingest complete.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
