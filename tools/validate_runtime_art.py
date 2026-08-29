#!/usr/bin/env python3
"""Fail CI if Pocket Familiar runtime art is missing or structurally truncated.

The Android runtime performs the stricter equal-cell/frame-count validation.
This script catches the failure that slipped through PR #12: a Gradle build can
succeed even when packaged WebP resources are incomplete/corrupt.
"""
from __future__ import annotations

from pathlib import Path
import struct
import sys

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PACKS = {
    "emi": ("idle", "walk", "run", "jump_land", "special", "recover", "sleep"),
    "kaelani": ("idle", "walk", "run", "jump_land", "special", "happy", "sleep"),
    "mira": ("idle", "walk", "run", "jump_land", "special", "happy", "sleep"),
}

STORY_PANELS = [*range(1, 16), 17, 18]


def validate_webp(path: Path) -> None:
    data = path.read_bytes()
    if len(data) < 20:
        raise ValueError(f"{path}: too small to be a WebP ({len(data)} bytes)")
    if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        raise ValueError(f"{path}: invalid RIFF/WEBP header")
    declared = struct.unpack_from("<I", data, 4)[0] + 8
    if declared != len(data):
        raise ValueError(
            f"{path}: truncated/oversized WebP (header={declared}, actual={len(data)})"
        )


def main() -> int:
    failures: list[str] = []

    for character, actions in PACKS.items():
        for action in actions:
            path = DRAWABLE / f"{character}_anim_{action}.webp"
            try:
                if not path.exists():
                    raise FileNotFoundError(f"missing {path.relative_to(ROOT)}")
                validate_webp(path)
            except (OSError, ValueError) as exc:
                failures.append(str(exc))

    for number in STORY_PANELS:
        path = DRAWABLE / f"story_ep0_{number:02d}.webp"
        try:
            if not path.exists():
                raise FileNotFoundError(f"missing {path.relative_to(ROOT)}")
            validate_webp(path)
        except (OSError, ValueError) as exc:
            failures.append(str(exc))

    if failures:
        print("Runtime art validation FAILED:", file=sys.stderr)
        for failure in failures:
            print(f"  - {failure}", file=sys.stderr)
        return 2

    print("Runtime art validation passed: 21 EMK strips + 17 Episode 0 panels.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
