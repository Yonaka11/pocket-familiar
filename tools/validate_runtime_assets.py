#!/usr/bin/env python3
"""Fail CI when Pocket Familiar ships missing, truncated, or mis-sized runtime art."""

from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

EXPECTED = {
    "emi": {"idle": 4, "walk": 4, "run": 4, "jump_land": 4, "special": 4, "recover": 3, "sleep": 2},
    "kaelani": {"idle": 4, "walk": 4, "run": 4, "jump_land": 4, "special": 4, "happy": 3, "sleep": 2},
    "mira": {"idle": 4, "walk": 4, "run": 4, "jump_land": 4, "special": 4, "happy": 3, "sleep": 2},
}


def load(path: Path) -> tuple[int, int]:
    with Image.open(path) as image:
        image.load()  # full decode, catches truncated PNG/WebP files
        return image.size


def main() -> int:
    errors: list[str] = []

    for character, actions in EXPECTED.items():
        for action, frames in actions.items():
            path = DRAWABLE / f"{character}_anim_{action}.png"
            if not path.exists():
                errors.append(f"missing {path.relative_to(ROOT)}")
                continue
            try:
                width, height = load(path)
            except Exception as exc:
                errors.append(f"cannot decode {path.relative_to(ROOT)}: {exc}")
                continue
            if height <= 0 or width != height * frames:
                errors.append(
                    f"bad strip geometry {path.relative_to(ROOT)}: {width}x{height}; "
                    f"expected {frames} square cells"
                )

    # Episode 0 intentionally has no panel 16: that memory-fragment card is rendered by Compose.
    for index in [*range(1, 16), 17, 18]:
        path = DRAWABLE / f"story_ep0_{index:02d}.webp"
        if not path.exists():
            errors.append(f"missing {path.relative_to(ROOT)}")
            continue
        try:
            load(path)
        except Exception as exc:
            errors.append(f"cannot decode {path.relative_to(ROOT)}: {exc}")

    if errors:
        print("Runtime asset validation FAILED:")
        for error in errors:
            print(f"  - {error}")
        return 1

    print("Runtime asset validation passed: EMK strips + Episode 0 panels decode cleanly.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
