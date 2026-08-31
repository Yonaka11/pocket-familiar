#!/usr/bin/env python3
"""Fail CI when Pocket Familiar ships broken or fake runtime art."""

from __future__ import annotations

from hashlib import sha256
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

EXPECTED = {
    "emi": {"idle": 4, "walk": 6, "run": 6, "jump_land": 4, "special": 6, "recover": 4, "sleep": 3},
    "kaelani": {"idle": 4, "walk": 6, "run": 6, "jump_land": 4, "special": 6, "happy": 4, "sleep": 3},
    "mira": {"idle": 4, "walk": 6, "run": 6, "jump_land": 4, "special": 6, "happy": 4, "sleep": 3},
}

EXPECTED_CELL_SIZE = 200
MIN_EDGE_MARGIN = 2


def load_rgba(path: Path) -> Image.Image:
    with Image.open(path) as image:
        image.load()  # full decode, catches truncated PNG/WebP files
        return image.convert("RGBA")


def frame_digest(frame: Image.Image) -> str:
    return sha256(frame.tobytes()).hexdigest()


def main() -> int:
    errors: list[str] = []

    for character, actions in EXPECTED.items():
        action_file_digests: dict[str, str] = {}

        for action, frames in actions.items():
            path = DRAWABLE / f"{character}_anim_{action}.png"
            if not path.exists():
                errors.append(f"missing {path.relative_to(ROOT)}")
                continue

            try:
                image = load_rgba(path)
            except Exception as exc:
                errors.append(f"cannot decode {path.relative_to(ROOT)}: {exc}")
                continue

            width, height = image.size
            if height <= 0 or width != height * frames:
                errors.append(
                    f"bad strip geometry {path.relative_to(ROOT)}: {width}x{height}; "
                    f"expected {frames} square cells"
                )
                continue
            if height != EXPECTED_CELL_SIZE:
                errors.append(
                    f"bad cell size {path.relative_to(ROOT)}: {height}px; "
                    f"expected {EXPECTED_CELL_SIZE}px"
                )
                continue

            action_file_digests[action] = sha256(path.read_bytes()).hexdigest()
            cell_width = width // frames
            frame_digests: list[str] = []

            for index in range(frames):
                frame = image.crop((index * cell_width, 0, (index + 1) * cell_width, height))
                alpha = frame.getchannel("A")
                if alpha.getbbox() is None:
                    errors.append(
                        f"blank animation cell {path.relative_to(ROOT)} frame {index + 1}/{frames}"
                    )
                else:
                    left, top, right, bottom = alpha.getbbox()
                    if (
                        left < MIN_EDGE_MARGIN
                        or top < MIN_EDGE_MARGIN
                        or right > cell_width - MIN_EDGE_MARGIN
                        or bottom > height - MIN_EDGE_MARGIN
                    ):
                        errors.append(
                            f"clipped/unsafe edge margin {path.relative_to(ROOT)} "
                            f"frame {index + 1}/{frames}: alpha bounds "
                            f"{(left, top, right, bottom)}"
                        )
                    opaque_pixels = alpha.histogram()[255]
                    if opaque_pixels > int(cell_width * height * 0.9):
                        errors.append(
                            f"likely opaque background {path.relative_to(ROOT)} "
                            f"frame {index + 1}/{frames}"
                        )
                frame_digests.append(frame_digest(frame))

            unique_frames = len(set(frame_digests))
            if unique_frames != frames:
                errors.append(
                    f"duplicate animation cells {path.relative_to(ROOT)}: "
                    f"{unique_frames}/{frames} frames are unique"
                )

        # The core locomotion states must be genuinely different strips. This
        # catches accidental copy/paste fallbacks such as a run file that is
        # byte-identical to walk or idle even though all files technically decode.
        locomotion = [name for name in ("idle", "walk", "run") if name in action_file_digests]
        digests = [action_file_digests[name] for name in locomotion]
        if len(digests) != len(set(digests)):
            errors.append(
                f"{character} locomotion strips are duplicated: idle/walk/run must be distinct"
            )

    # Episode 0 intentionally has no panel 16: that memory-fragment card is rendered by Compose.
    for index in [*range(1, 16), 17, 18]:
        path = DRAWABLE / f"story_ep0_{index:02d}.webp"
        if not path.exists():
            errors.append(f"missing {path.relative_to(ROOT)}")
            continue
        try:
            image = load_rgba(path)
            if image.width <= 0 or image.height <= 0:
                errors.append(f"empty story panel {path.relative_to(ROOT)}")
        except Exception as exc:
            errors.append(f"cannot decode {path.relative_to(ROOT)}: {exc}")

    if errors:
        print("Runtime asset validation FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print(
        "Runtime asset validation passed: all EMK strips fully decode, "
        "every animation cell is populated and unique, locomotion strips are distinct, "
        "and Episode 0 panels decode cleanly."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
