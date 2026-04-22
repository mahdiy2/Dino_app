#!/usr/bin/env python3
"""
Generate stylized Play Store screenshots from raw captures in `screen/`.

Output files are written to `screen/play_store_ready`.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont, ImageOps

try:
    RESAMPLE = Image.Resampling.LANCZOS  # Pillow >= 9
except AttributeError:  # pragma: no cover - compatibility for older Pillow.
    RESAMPLE = Image.LANCZOS


ROOT = Path(__file__).resolve().parents[1]
SCREEN_DIR = ROOT / "screen"
OUT_DIR = SCREEN_DIR / "play_store_ready"

CANVAS_SIZE = (1440, 3200)
SHOT_MAX_SIZE = (1080, 2280)


@dataclass(frozen=True)
class ScreenText:
    title: str
    subtitle: str


SCREEN_COPY: list[ScreenText] = [
    ScreenText("Post Music In Seconds", "Share tracks and let friends react instantly."),
    ScreenText("Smooth Media Controls", "Play, pause, and preview from one clean composer."),
    ScreenText("Create Rich Dives", "Build immersive posts with images, audio, and tags."),
    ScreenText("Showcase Your Vibe", "Arrange media cards and publish with confidence."),
    ScreenText("Discover What Matters", "Browse trending posts in a distraction-free feed."),
    ScreenText("Listen While You Scroll", "Inline player keeps your session flowing."),
    ScreenText("Stay On Top Of Updates", "Notifications keep every interaction in reach."),
    ScreenText("Quick Actions, No Clutter", "Reply or jump in with fast shortcuts."),
    ScreenText("Find People Fast", "Smart search helps you reach the right contact."),
    ScreenText("Organized Conversations", "Clean inbox design for stress-free messaging."),
    ScreenText("Expressive Private Chat", "Send media, react, and keep talks alive."),
    ScreenText("Media That Pops", "Share photos and moments in full focus."),
]


PALETTES: list[tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]]] = [
    ((14, 22, 57), (17, 104, 204), (105, 82, 255)),
    ((18, 15, 42), (133, 44, 184), (61, 188, 255)),
    ((8, 29, 48), (0, 167, 142), (0, 105, 184)),
    ((34, 15, 33), (255, 91, 121), (94, 87, 255)),
    ((20, 20, 20), (55, 169, 255), (106, 238, 191)),
    ((16, 17, 54), (255, 142, 57), (242, 76, 123)),
]


def load_font(candidates: Iterable[str], size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for path in candidates:
        p = Path(path)
        if p.exists():
            return ImageFont.truetype(str(p), size=size)
    return ImageFont.load_default()


def wrap_text(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont, max_width: int) -> str:
    words = text.split()
    lines: list[str] = []
    current: list[str] = []
    for word in words:
        trial = " ".join(current + [word])
        left, _, right, _ = draw.textbbox((0, 0), trial, font=font)
        if right - left <= max_width or not current:
            current.append(word)
        else:
            lines.append(" ".join(current))
            current = [word]
    if current:
        lines.append(" ".join(current))
    return "\n".join(lines)


def make_background(size: tuple[int, int], palette: tuple[tuple[int, int, int], tuple[int, int, int], tuple[int, int, int]]) -> Image.Image:
    w, h = size
    c0, c1, c2 = palette
    vertical = Image.linear_gradient("L").resize((w, h))
    bg = ImageOps.colorize(vertical, black=c0, white=c1).convert("RGBA")
    horizontal = Image.linear_gradient("L").rotate(90, expand=True).resize((w, h))
    tint = ImageOps.colorize(horizontal, black=(0, 0, 0), white=c2).convert("RGBA")
    tint.putalpha(75)
    bg = Image.alpha_composite(bg, tint)

    overlay = Image.new("RGBA", size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)

    # Soft glows that add depth without making text hard to read.
    d.ellipse((-300, -200, 980, 920), fill=(*c2, 85))
    d.ellipse((500, 440, 1660, 1700), fill=(*c1, 72))
    d.ellipse((-350, 1800, 950, 3300), fill=(255, 255, 255, 24))
    overlay = overlay.filter(ImageFilter.GaussianBlur(100))
    bg = Image.alpha_composite(bg.convert("RGBA"), overlay).convert("RGB")

    # Subtle scan lines for texture.
    texture = Image.new("RGBA", size, (0, 0, 0, 0))
    td = ImageDraw.Draw(texture)
    for y in range(0, h, 8):
        td.line([(0, y), (w, y)], fill=(255, 255, 255, 7), width=1)
    return Image.alpha_composite(bg.convert("RGBA"), texture).convert("RGBA")


def rounded_image(im: Image.Image, radius: int) -> Image.Image:
    if im.mode != "RGBA":
        im = im.convert("RGBA")
    alpha = Image.new("L", im.size, 0)
    d = ImageDraw.Draw(alpha)
    d.rounded_rectangle((0, 0, im.width, im.height), radius=radius, fill=255)
    im.putalpha(alpha)
    return im


def add_phone_shot(base: Image.Image, shot: Image.Image) -> None:
    shot = shot.convert("RGBA")
    shot.thumbnail(SHOT_MAX_SIZE, RESAMPLE)
    shot = rounded_image(shot, radius=58)

    border = Image.new("RGBA", (shot.width + 24, shot.height + 24), (0, 0, 0, 0))
    d = ImageDraw.Draw(border)
    d.rounded_rectangle((0, 0, border.width - 1, border.height - 1), radius=68, fill=(255, 255, 255, 20), outline=(255, 255, 255, 130), width=3)
    border.paste(shot, (12, 12), shot)

    # Shadow from the alpha channel for a floating-card effect.
    alpha = border.split()[-1]
    shadow = Image.new("RGBA", border.size, (0, 0, 0, 0))
    shadow.putalpha(alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(30))
    shadow = ImageChops.multiply(shadow, Image.new("RGBA", border.size, (0, 0, 0, 170)))

    x = (base.width - border.width) // 2
    y = 780
    base.alpha_composite(shadow, (x, y + 34))
    base.alpha_composite(border, (x, y))


def add_copy(base: Image.Image, screen_text: ScreenText, index: int, title_font: ImageFont.ImageFont, body_font: ImageFont.ImageFont) -> None:
    draw = ImageDraw.Draw(base)

    badge = "SEC MESSAGE"
    badge_w = draw.textbbox((0, 0), badge, font=body_font)[2]
    badge_x = (base.width - badge_w - 88) // 2
    badge_y = 116

    draw.rounded_rectangle((badge_x, badge_y, badge_x + badge_w + 88, badge_y + 82), radius=41, fill=(255, 255, 255, 30), outline=(255, 255, 255, 110), width=2)
    draw.text((badge_x + 44, badge_y + 22), badge, font=body_font, fill=(232, 240, 255, 255))

    title = wrap_text(draw, screen_text.title, title_font, max_width=1180)
    subtitle = wrap_text(draw, screen_text.subtitle, body_font, max_width=1140)

    tw_l, tw_t, tw_r, tw_b = draw.multiline_textbbox((0, 0), title, font=title_font, align="center", spacing=8)
    title_w = tw_r - tw_l
    title_h = tw_b - tw_t
    draw.multiline_text(((base.width - title_w) // 2, 250), title, font=title_font, fill=(246, 250, 255, 255), align="center", spacing=8)

    sw_l, sw_t, sw_r, sw_b = draw.multiline_textbbox((0, 0), subtitle, font=body_font, align="center", spacing=6)
    subtitle_w = sw_r - sw_l
    draw.multiline_text(((base.width - subtitle_w) // 2, 282 + title_h), subtitle, font=body_font, fill=(216, 228, 244, 255), align="center", spacing=6)

    footer = f"{index + 1:02d}"
    fw = draw.textbbox((0, 0), footer, font=body_font)[2]
    draw.text((base.width - fw - 66, base.height - 120), footer, font=body_font, fill=(240, 246, 255, 170))


def main() -> None:
    shots = sorted(p for p in SCREEN_DIR.glob("*.png") if p.is_file())
    if not shots:
        raise SystemExit("No PNG screenshots were found in screen/.")

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    title_font = load_font(
        [
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
        ],
        size=94,
    )
    body_font = load_font(
        [
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        ],
        size=46,
    )

    for index, shot_path in enumerate(shots):
        copy_block = SCREEN_COPY[index % len(SCREEN_COPY)]
        palette = PALETTES[index % len(PALETTES)]

        with Image.open(shot_path) as shot:
            canvas = make_background(CANVAS_SIZE, palette)
            add_copy(canvas, copy_block, index, title_font, body_font)
            add_phone_shot(canvas, shot)

            out_name = f"play_{index + 1:02d}.png"
            out_path = OUT_DIR / out_name
            canvas.convert("RGB").save(out_path, optimize=True)
            print(f"created {out_path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
try:
    RESAMPLE = Image.Resampling.LANCZOS  # Pillow >= 9
except AttributeError:  # pragma: no cover - compatibility for older Pillow.
    RESAMPLE = Image.LANCZOS
