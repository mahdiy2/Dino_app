#!/usr/bin/env python3
"""
Normalize images in a folder to meet store-ready constraints.

Rules enforced per image:
- Maximum dimension: 3000 px
- Allowed aspect ratios: 16:9 or 9:16 (padding with white background)
- Maximum file size: 3 MB

Supported formats: JPG, JPEG, PNG, WEBP
"""

from __future__ import annotations

import argparse
import math
import shutil
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path

from PIL import Image

try:
    RESAMPLE = Image.Resampling.LANCZOS  # Pillow >= 9
except AttributeError:  # pragma: no cover - compatibility for older Pillow.
    RESAMPLE = Image.LANCZOS


MAX_DIMENSION = 3000
MAX_BYTES = 3 * 1024 * 1024
ALLOWED_RATIOS: tuple[tuple[int, int], ...] = ((16, 9), (9, 16))
SUPPORTED_EXTENSIONS = {".jpg": "JPEG", ".jpeg": "JPEG", ".png": "PNG", ".webp": "WEBP"}


@dataclass
class ProcessResult:
    resized: bool = False
    padded: bool = False
    compressed: bool = False
    already_valid: bool = False
    skipped: bool = False
    reason: str = ""


def ratio_is_allowed(width: int, height: int) -> bool:
    return any(width * b == height * a for a, b in ALLOWED_RATIOS)


def flatten_to_rgb(image: Image.Image) -> Image.Image:
    """Flatten transparency onto white so padding/exports are consistent."""
    if image.mode == "RGB":
        return image.copy()

    if image.mode in ("RGBA", "LA") or "transparency" in image.info:
        rgba = image.convert("RGBA")
        base = Image.new("RGB", rgba.size, (255, 255, 255))
        base.paste(rgba, mask=rgba.getchannel("A"))
        return base

    return image.convert("RGB")


def resize_if_needed(image: Image.Image) -> tuple[Image.Image, bool]:
    width, height = image.size
    largest = max(width, height)
    if largest <= MAX_DIMENSION:
        return image, False

    scale = MAX_DIMENSION / float(largest)
    new_size = (
        max(1, int(round(width * scale))),
        max(1, int(round(height * scale))),
    )
    return image.resize(new_size, RESAMPLE), True


def canvas_for_ratio(width: int, height: int, ratio_w: int, ratio_h: int) -> tuple[int, int]:
    # Exact ratio canvas: (ratio_w * k, ratio_h * k), large enough to hold image.
    k = max(math.ceil(width / ratio_w), math.ceil(height / ratio_h))
    return ratio_w * k, ratio_h * k


def pad_to_allowed_ratio(image: Image.Image) -> tuple[Image.Image, bool]:
    width, height = image.size
    if ratio_is_allowed(width, height):
        return image, False

    image_ratio = width / float(height)
    candidates: list[tuple[int, float, tuple[int, int]]] = []
    for ratio_w, ratio_h in ALLOWED_RATIOS:
        canvas_w, canvas_h = canvas_for_ratio(width, height, ratio_w, ratio_h)
        extra_area = (canvas_w * canvas_h) - (width * height)
        ratio_delta = abs(image_ratio - (ratio_w / float(ratio_h)))
        candidates.append((extra_area, ratio_delta, (canvas_w, canvas_h)))

    candidates.sort(key=lambda item: (item[0], item[1]))
    canvas_w, canvas_h = candidates[0][2]

    padded = Image.new("RGB", (canvas_w, canvas_h), (255, 255, 255))
    offset = ((canvas_w - width) // 2, (canvas_h - height) // 2)
    padded.paste(image, offset)
    return padded, True


def encode_jpeg(image: Image.Image, quality: int) -> bytes:
    buf = BytesIO()
    image.save(
        buf,
        format="JPEG",
        quality=quality,
        optimize=True,
        progressive=True,
        subsampling="4:2:0",
    )
    return buf.getvalue()


def encode_webp(image: Image.Image, quality: int) -> bytes:
    buf = BytesIO()
    image.save(
        buf,
        format="WEBP",
        quality=quality,
        method=6,
    )
    return buf.getvalue()


def encode_png(image: Image.Image, colors: int | None = None) -> bytes:
    buf = BytesIO()
    payload = image
    if colors is not None:
        payload = image.convert("P", palette=Image.ADAPTIVE, colors=colors)
    payload.save(buf, format="PNG", optimize=True, compress_level=9)
    return buf.getvalue()


def try_encode_under_limit(image: Image.Image, target_format: str) -> tuple[bytes, bool]:
    """Return encoded bytes and whether size-limit compression was required."""
    if target_format == "JPEG":
        initial = encode_jpeg(image, 95)
        if len(initial) <= MAX_BYTES:
            return initial, False
        attempts = [encode_jpeg(image, q) for q in (90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 25, 20)]
    elif target_format == "WEBP":
        initial = encode_webp(image, 95)
        if len(initial) <= MAX_BYTES:
            return initial, False
        attempts = [encode_webp(image, q) for q in (90, 85, 80, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 25, 20)]
    else:  # PNG
        initial = encode_png(image, None)
        if len(initial) <= MAX_BYTES:
            return initial, False
        attempts = [encode_png(image, colors) for colors in (256, 192, 128, 96, 64, 48, 32, 16)]

    for payload in attempts:
        if len(payload) <= MAX_BYTES:
            return payload, True

    # If still too large, progressively downscale and retry stronger options.
    smallest = min([initial, *attempts], key=len)
    resized = image
    for _ in range(10):
        next_w = max(1, int(resized.width * 0.9))
        next_h = max(1, int(resized.height * 0.9))
        if next_w == resized.width and next_h == resized.height:
            break
        resized = resized.resize((next_w, next_h), RESAMPLE)

        if target_format == "JPEG":
            retries = [encode_jpeg(resized, q) for q in (70, 60, 50, 40, 35, 30, 25, 20)]
        elif target_format == "WEBP":
            retries = [encode_webp(resized, q) for q in (70, 60, 50, 40, 35, 30, 25, 20)]
        else:
            retries = [encode_png(resized, c) for c in (128, 96, 64, 48, 32, 16)]

        for payload in retries:
            if len(payload) <= MAX_BYTES:
                return payload, True
        smallest = min([smallest, *retries], key=len)

    return smallest, True


def process_one(path: Path, out_dir: Path) -> ProcessResult:
    result = ProcessResult()
    extension = path.suffix.lower()
    target_format = SUPPORTED_EXTENSIONS.get(extension)
    if target_format is None:
        return ProcessResult(skipped=True, reason="unsupported format")

    output_path = out_dir / path.name

    try:
        with Image.open(path) as raw:
            width, height = raw.size
            ratio_ok = ratio_is_allowed(width, height)
            size_ok = path.stat().st_size <= MAX_BYTES
            dimension_ok = max(width, height) <= MAX_DIMENSION

            if ratio_ok and size_ok and dimension_ok:
                out_dir.mkdir(parents=True, exist_ok=True)
                shutil.copy2(path, output_path)
                return ProcessResult(already_valid=True)

            image = flatten_to_rgb(raw)
    except Exception as exc:  # pragma: no cover - unexpected decode failures
        return ProcessResult(skipped=True, reason=f"open failure: {exc}")

    image, result.resized = resize_if_needed(image)
    image, result.padded = pad_to_allowed_ratio(image)

    payload, required_compression = try_encode_under_limit(image, target_format)
    result.compressed = required_compression

    out_dir.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(payload)
    return result


def iter_input_files(input_dir: Path) -> list[Path]:
    return sorted([p for p in input_dir.iterdir() if p.is_file()])


def main() -> None:
    parser = argparse.ArgumentParser(description="Normalize images in a folder.")
    parser.add_argument(
        "input_dir",
        nargs="?",
        default="screen",
        help="Input folder with images (default: screen)",
    )
    parser.add_argument(
        "output_dir",
        nargs="?",
        default="screen/processed",
        help="Output folder for normalized images (default: screen/processed)",
    )
    args = parser.parse_args()

    input_dir = Path(args.input_dir).resolve()
    output_dir = Path(args.output_dir).resolve()

    if not input_dir.exists() or not input_dir.is_dir():
        raise SystemExit(f"Input directory not found: {input_dir}")

    files = iter_input_files(input_dir)
    if not files:
        raise SystemExit(f"No files found in: {input_dir}")

    resized_files: list[str] = []
    padded_files: list[str] = []
    compressed_files: list[str] = []
    already_valid_files: list[str] = []
    skipped_files: list[str] = []

    print(f"Processing {len(files)} files from {input_dir} -> {output_dir}")
    for path in files:
        result = process_one(path, output_dir)
        if result.skipped:
            skipped_files.append(f"{path.name} ({result.reason})")
            print(f"- {path.name}: skipped ({result.reason})")
            continue

        actions: list[str] = []
        if result.already_valid:
            already_valid_files.append(path.name)
            actions.append("already valid")
        if result.resized:
            resized_files.append(path.name)
            actions.append("resized")
        if result.padded:
            padded_files.append(path.name)
            actions.append("padded")
        if result.compressed:
            compressed_files.append(path.name)
            actions.append("compressed")

        print(f"- {path.name}: {', '.join(actions) if actions else 'processed'}")

    print("\nSummary:")
    print(f"  resized: {len(resized_files)}")
    print(f"  padded: {len(padded_files)}")
    print(f"  compressed: {len(compressed_files)}")
    print(f"  already valid: {len(already_valid_files)}")
    print(f"  skipped: {len(skipped_files)}")

    if resized_files:
        print("  resized files:", ", ".join(resized_files))
    if padded_files:
        print("  padded files:", ", ".join(padded_files))
    if compressed_files:
        print("  compressed files:", ", ".join(compressed_files))
    if already_valid_files:
        print("  already valid files:", ", ".join(already_valid_files))
    if skipped_files:
        print("  skipped files:", ", ".join(skipped_files))


if __name__ == "__main__":
    main()
