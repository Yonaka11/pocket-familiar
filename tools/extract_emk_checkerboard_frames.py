#!/usr/bin/env python3
"""Conservatively remove baked checkerboards from approved EMK frame art.

The extractor never edits retained RGB pixels. It infers the two checker colors
and their spatial parity from each image border, marks only close neutral matches
as background candidates, then clears only candidates connected to an outer edge.
Anything uncertain stays opaque for human review.

Most frames are processed from exact grid crops. Actions whose complete poses cross
an imaginary grid divider are processed from the full sheet: connected foreground
components are assigned to the nearest main pose and copied, byte-for-byte, onto a
padded transparent canvas. No character pixels are invented, keyed globally, or
rescaled.
"""

from __future__ import annotations

import json
from hashlib import sha256
import os
from pathlib import Path
import sys

import numpy as np
from PIL import Image
from scipy import ndimage
from scipy.optimize import linear_sum_assignment


ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT.parent / "animation_work"
FRAME_INPUT = WORK / "frame_alpha_input"
OUTPUT = ROOT / "art_source" / "runtime_frames"
AUDIT_PATH = ROOT / "docs" / "art" / "EMK_BACKGROUND_EXTRACTION_AUDIT.json"

CHARACTERS = {
    "emi": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("recover", 4), ("sleep", 3)],
    "kaelani": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("happy", 4), ("sleep", 3)],
    "mira": [("idle", 4), ("walk", 6), ("run", 6), ("jump_land", 4), ("special", 6), ("happy", 4), ("sleep", 3)],
}
GRIDS = {3: (3, 1), 4: (2, 2), 6: (3, 2)}

# These accepted poses cross equal-cell dividers but remain complete and separate
# on the full source sheet. Full-sheet component assignment preserves every pixel.
WHOLE_SHEET_ACTIONS = {
    ("kaelani", "jump_land"),
    ("kaelani", "run"),
    ("kaelani", "sleep"),
    ("kaelani", "special"),
    ("mira", "special"),
}

MIN_BACKGROUND_CHANNEL = 215
MAX_BACKGROUND_CHROMA = 18
MAX_CENTER_DISTANCE = 24.0
MIN_COMPONENT_AREA = 4
PAD = 32

# Human-reviewed geometric exclusions for isolated checkerboard remnants. These
# are deliberately frame-specific: detached spell/reaction effects must never be
# removed by a generalized component-size rule.
GEOMETRIC_EXCLUSIONS = {
    ("emi", "run", 0): [
        {
            "bounds": [11, 9, 15, 13],
            "expected_pixels": 11,
            "reason": "isolated checker-colored component, 116 px from main pose",
        }
    ],
    ("kaelani", "jump_land", 0): [
        {
            "bounds": [310, 546, 311, 548],
            "expected_pixels": 2,
            "reason": "isolated neutral background residue, 57 px below main pose",
        }
    ],
    ("kaelani", "special", 1): [
        {
            "bounds": [278, 527, 283, 530],
            "expected_pixels": 10,
            "reason": "isolated neutral background residue, 22 px below main pose",
        }
    ],
    ("mira", "jump_land", 3): [
        {
            "bounds": [425, 184, 430, 185],
            "expected_pixels": 5,
            "reason": "isolated neutral background residue, 10 px right of main pose",
        }
    ],
}


def sheet_path(character: str, action: str) -> Path:
    source = WORK / ("clean_attempt/emi" if character == "emi" else f"generated/{character}")
    return source / f"{action}_sheet.png"


def load_rgb(path: Path) -> np.ndarray:
    with Image.open(path) as opened:
        opened.load()
        return np.asarray(opened.convert("RGB"), dtype=np.uint8)


def smooth_binary(values: np.ndarray, radius: int = 2) -> np.ndarray:
    padded = np.pad(values.astype(np.uint8), radius, mode="edge")
    total = np.zeros(values.shape, dtype=np.uint16)
    for offset in range(radius * 2 + 1):
        total += padded[offset : offset + values.size]
    return total > radius


def checker_model(rgb: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    height, width, _ = rgb.shape
    border = np.concatenate((rgb[0], rgb[-1], rgb[:, 0], rgb[:, -1]), axis=0).astype(np.float32)
    border_min = border.min(axis=1)
    border_chroma = border.max(axis=1) - border_min
    neutral = border[(border_min >= MIN_BACKGROUND_CHANNEL) & (border_chroma <= MAX_BACKGROUND_CHROMA)]
    if neutral.shape[0] < max(64, (width + height) // 4):
        raise ValueError("not enough neutral border samples to infer checkerboard")

    luminance = neutral.mean(axis=1)
    low, high = np.percentile(luminance, [25, 75])
    for _ in range(12):
        split = np.abs(luminance - low) <= np.abs(luminance - high)
        if split.all() or (~split).all():
            break
        low = float(luminance[split].mean())
        high = float(luminance[~split].mean())
    if low > high:
        low, high = high, low
    threshold = (low + high) / 2.0

    top_lum = rgb[0].astype(np.float32).mean(axis=1)
    left_lum = rgb[:, 0].astype(np.float32).mean(axis=1)
    x_light = smooth_binary(top_lum > threshold)
    left_light = smooth_binary(left_lum > threshold)
    # Spatial parity at (x,y) is the XOR of horizontal changes from the top-left
    # and vertical changes from the same top-left reference.
    y_toggle = left_light ^ left_light[0]
    expected_light = x_light[None, :] ^ y_toggle[:, None]

    all_pixels = rgb.reshape(-1, 3).astype(np.float32)
    all_lum = all_pixels.mean(axis=1)
    all_min = all_pixels.min(axis=1)
    all_chroma = all_pixels.max(axis=1) - all_min
    probable = (all_min >= MIN_BACKGROUND_CHANNEL) & (all_chroma <= MAX_BACKGROUND_CHROMA)
    probable_pixels = all_pixels[probable]
    probable_light = all_lum[probable] > threshold
    centers = []
    for is_light in (False, True):
        group = probable_pixels[probable_light == is_light]
        if group.size == 0:
            raise ValueError("could not infer both checkerboard colors")
        centers.append(np.median(group, axis=0))
    return np.asarray(centers, dtype=np.float32), expected_light, probable.reshape(height, width)


def extract_alpha(rgb: np.ndarray) -> tuple[np.ndarray, dict[str, object]]:
    centers, expected_light, probable = checker_model(rgb)
    expected = centers[expected_light.astype(np.uint8)]
    distance = np.linalg.norm(rgb.astype(np.float32) - expected, axis=2)
    candidate = probable & (distance <= MAX_CENTER_DISTANCE)

    edge_seed = np.zeros(candidate.shape, dtype=bool)
    edge_seed[0] = candidate[0]
    edge_seed[-1] = candidate[-1]
    edge_seed[:, 0] = candidate[:, 0]
    edge_seed[:, -1] = candidate[:, -1]
    clear = ndimage.binary_propagation(edge_seed, mask=candidate)
    alpha = np.where(clear, 0, 255).astype(np.uint8)
    stats = {
        "checker_centers": [[round(float(channel), 3) for channel in center] for center in centers],
        "background_candidates": int(candidate.sum()),
        "cleared_pixels": int(clear.sum()),
        "retained_pixels": int((~clear).sum()),
    }
    return alpha, stats


def edge_contacts(mask: np.ndarray) -> dict[str, int]:
    contacts = {
        "top": int(mask[0].sum()),
        "bottom": int(mask[-1].sum()),
        "left": int(mask[:, 0].sum()),
        "right": int(mask[:, -1].sum()),
    }
    return {name: count for name, count in contacts.items() if count}


def rgb_digest(pixels: np.ndarray) -> str:
    return sha256(np.ascontiguousarray(pixels).tobytes()).hexdigest()


def save_rgba(rgb: np.ndarray, alpha: np.ndarray, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    rgba = np.dstack((rgb, alpha))
    image = Image.fromarray(rgba, "RGBA")
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


def apply_geometric_exclusions(
    character: str,
    action: str,
    index: int,
    alpha: np.ndarray,
    source: Path,
) -> list[dict[str, object]]:
    """Clear only explicitly reviewed, fully isolated background components."""
    exclusions = []
    for exclusion in GEOMETRIC_EXCLUSIONS.get((character, action, index), []):
        left, top, right, bottom = exclusion["bounds"]
        region = alpha[top:bottom, left:right]
        excluded_pixels = int((region != 0).sum())
        if excluded_pixels != exclusion["expected_pixels"]:
            raise ValueError(
                f"{source}: reviewed exclusion {exclusion['bounds']} expected "
                f"{exclusion['expected_pixels']} opaque pixels, found {excluded_pixels}"
            )
        labels, component_count = ndimage.label(alpha != 0, structure=np.ones((3, 3), dtype=np.uint8))
        component_ids = np.unique(labels[top:bottom, left:right])
        component_ids = component_ids[component_ids != 0]
        if component_count < 2 or component_ids.size != 1:
            raise ValueError(f"{source}: reviewed exclusion no longer isolates one component")
        component_mask = labels == int(component_ids[0])
        component_y, component_x = np.where(component_mask)
        component_bounds = [
            int(component_x.min()),
            int(component_y.min()),
            int(component_x.max()) + 1,
            int(component_y.max()) + 1,
        ]
        if component_bounds != exclusion["bounds"] or int(component_mask.sum()) != excluded_pixels:
            raise ValueError(f"{source}: reviewed exclusion would clear only part of a component")
        alpha[component_mask] = 0
        exclusions.append(
            {
                **exclusion,
                "excluded_pixels": excluded_pixels,
                "rgb_changed": False,
            }
        )
    return exclusions


def process_grid_frame(character: str, action: str, index: int) -> dict[str, object]:
    source = FRAME_INPUT / character / action / f"{index:02d}.png"
    rgb = load_rgb(source)
    alpha, stats = extract_alpha(rgb)
    exclusions = apply_geometric_exclusions(character, action, index, alpha, source)
    retained = alpha != 0
    contacts = edge_contacts(retained)
    if contacts:
        raise ValueError(f"{source}: retained foreground touches crop edge: {contacts}")

    destination = OUTPUT / character / action / f"{index:02d}.png"
    save_rgba(rgb, alpha, destination)
    output_rgb = load_rgb(destination)
    if not np.array_equal(output_rgb[retained], rgb[retained]):
        raise AssertionError(f"retained RGB changed while saving {destination}")
    digest = rgb_digest(rgb[retained])
    return {
        "character": character,
        "action": action,
        "frame": index,
        "mode": "equal_grid_crop",
        "source": str(source.relative_to(WORK)),
        "source_size": [int(rgb.shape[1]), int(rgb.shape[0])],
        "output_size": [int(rgb.shape[1]), int(rgb.shape[0])],
        "source_edge_contacts": contacts,
        "retained_rgb_sha256": digest,
        "output_rgb_sha256": digest,
        "retained_rgb_byte_identical": True,
        "geometric_exclusions": exclusions,
        **stats,
        "retained_pixels_after_exclusions": int(retained.sum()),
    }


def main_pose_components(labels: np.ndarray, count: int) -> tuple[list[int], list[tuple[float, float]]]:
    areas = np.bincount(labels.ravel())
    component_ids = np.argsort(areas[1:])[::-1][:count] + 1
    if len(component_ids) != count or np.any(areas[component_ids] < 1000):
        raise ValueError(f"could not identify {count} main pose components")
    centers = []
    for component_id in component_ids:
        ys, xs = np.where(labels == component_id)
        centers.append((float(xs.mean()), float(ys.mean())))
    return component_ids.tolist(), centers


def order_pose_components(
    centers: list[tuple[float, float]],
    width: int,
    height: int,
    count: int,
) -> list[int]:
    columns, rows = GRIDS[count]
    expected = [
        ((column + 0.5) * width / columns, (row + 0.5) * height / rows)
        for row in range(rows)
        for column in range(columns)
    ]
    costs = np.asarray(
        [
            [(x - ex) ** 2 + (y - ey) ** 2 for ex, ey in expected]
            for x, y in centers
        ],
        dtype=np.float64,
    )
    component_rows, frame_columns = linear_sum_assignment(costs)
    ordered = [-1] * count
    for component_index, frame_index in zip(component_rows, frame_columns, strict=True):
        ordered[int(frame_index)] = int(component_index)
    if any(index < 0 for index in ordered):
        raise ValueError("failed to order main pose components")
    return ordered


def process_full_sheet_action(character: str, action: str, count: int) -> list[dict[str, object]]:
    source = sheet_path(character, action)
    rgb = load_rgb(source)
    alpha, sheet_stats = extract_alpha(rgb)
    retained = alpha != 0
    contacts = edge_contacts(retained)
    if contacts:
        raise ValueError(f"{source}: retained foreground touches outer sheet edge: {contacts}")

    labels, component_count = ndimage.label(retained, structure=np.ones((3, 3), dtype=np.uint8))
    main_ids, main_centers = main_pose_components(labels, count)
    ordered_component_indexes = order_pose_components(main_centers, rgb.shape[1], rgb.shape[0], count)
    ordered_main_ids = [main_ids[index] for index in ordered_component_indexes]
    ordered_centers = [main_centers[index] for index in ordered_component_indexes]

    areas = np.bincount(labels.ravel())
    assigned: list[list[int]] = [[] for _ in range(count)]
    for component_id in range(1, component_count + 1):
        if areas[component_id] < MIN_COMPONENT_AREA:
            # Uncertain pixels must remain opaque, so even tiny components are
            # retained; their centroid is assigned to the nearest main pose.
            pass
        ys, xs = np.where(labels == component_id)
        if xs.size == 0:
            continue
        center_x, center_y = float(xs.mean()), float(ys.mean())
        frame_index = min(
            range(count),
            key=lambda index: (center_x - ordered_centers[index][0]) ** 2
            + (center_y - ordered_centers[index][1]) ** 2,
        )
        assigned[frame_index].append(component_id)

    results = []
    for index, component_ids in enumerate(assigned):
        frame_mask = np.isin(labels, component_ids)
        ys, xs = np.where(frame_mask)
        if xs.size == 0 or ordered_main_ids[index] not in component_ids:
            raise ValueError(f"{source}: frame {index:02d} lost its main pose component")
        left, top, right, bottom = int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1
        crop_rgb = rgb[top:bottom, left:right]
        crop_mask = frame_mask[top:bottom, left:right]
        side = max(crop_rgb.shape[0], crop_rgb.shape[1]) + PAD * 2
        offset_x = (side - crop_rgb.shape[1]) // 2
        offset_y = (side - crop_rgb.shape[0]) // 2
        output_rgb = np.zeros((side, side, 3), dtype=np.uint8)
        output_alpha = np.zeros((side, side), dtype=np.uint8)
        target_rgb = output_rgb[offset_y : offset_y + crop_rgb.shape[0], offset_x : offset_x + crop_rgb.shape[1]]
        target_alpha = output_alpha[offset_y : offset_y + crop_rgb.shape[0], offset_x : offset_x + crop_rgb.shape[1]]
        target_rgb[crop_mask] = crop_rgb[crop_mask]
        target_alpha[crop_mask] = 255
        exclusions = apply_geometric_exclusions(character, action, index, output_alpha, source)

        destination = OUTPUT / character / action / f"{index:02d}.png"
        save_rgba(output_rgb, output_alpha, destination)
        decoded_rgb = load_rgb(destination)
        output_retained = output_alpha != 0
        retained_source_mask = target_alpha != 0
        source_pixels = crop_rgb[retained_source_mask]
        output_pixels = decoded_rgb[output_retained]
        if not np.array_equal(output_pixels, source_pixels):
            raise AssertionError(f"retained RGB changed while composing {destination}")
        source_digest = rgb_digest(source_pixels)
        output_digest = rgb_digest(output_pixels)
        results.append(
            {
                "character": character,
                "action": action,
                "frame": index,
                "mode": "full_sheet_components",
                "source": str(source.relative_to(WORK)),
                "source_size": [int(rgb.shape[1]), int(rgb.shape[0])],
                "output_size": [side, side],
                "source_bounds": [left, top, right, bottom],
                "source_edge_contacts": contacts,
                "assigned_components": len(component_ids),
                "retained_pixels": int(output_retained.sum()),
                "retained_rgb_sha256": source_digest,
                "output_rgb_sha256": output_digest,
                "retained_rgb_byte_identical": source_digest == output_digest,
                "geometric_exclusions": exclusions,
                "sheet_background_candidates": sheet_stats["background_candidates"],
                "sheet_cleared_pixels": sheet_stats["cleared_pixels"],
                "checker_centers": sheet_stats["checker_centers"],
            }
        )
    return results


def main() -> int:
    audit: list[dict[str, object]] = []
    try:
        for character, actions in CHARACTERS.items():
            for action, count in actions:
                if (character, action) in WHOLE_SHEET_ACTIONS:
                    audit.extend(process_full_sheet_action(character, action, count))
                else:
                    audit.extend(process_grid_frame(character, action, index) for index in range(count))

        if len(audit) != 99:
            raise AssertionError(f"expected 99 extracted frames, got {len(audit)}")
        if not all(item["retained_rgb_byte_identical"] for item in audit):
            raise AssertionError("retained RGB byte-identity audit failed")

        OUTPUT.mkdir(parents=True, exist_ok=True)
        AUDIT_PATH.write_text(json.dumps({"frames": audit}, indent=2) + "\n", encoding="utf-8")
    except (AssertionError, FileNotFoundError, OSError, ValueError) as exc:
        print(f"Checkerboard extraction FAILED: {exc}", file=sys.stderr)
        return 2

    cleared = sum(int(item.get("cleared_pixels", 0)) for item in audit)
    print(
        f"Checkerboard extraction passed: {len(audit)} frames; retained RGB is byte-identical; "
        f"per-frame cleared pixels={cleared}; audit={AUDIT_PATH.relative_to(ROOT)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
