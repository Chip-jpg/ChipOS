#!/usr/bin/env python3
"""
Draws the ChipOS app icon and the Android TV banner (original artwork, MIT licensed with the repo).

    pip install pillow
    python tools/generate_icons.py

Writes:
  app/src/main/res/mipmap-xxhdpi/ic_launcher.png   (144x144)
  app/src/main/res/mipmap-xhdpi/ic_launcher.png    (96x96)
  app/src/main/res/drawable-xhdpi/banner.png       (640x360 = 320x180 dp)
"""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")
SCALE = 4  # draw big, then downsample for smooth edges

FONT_CANDIDATES = [
    "/usr/share/fonts/opentype/inter/Inter-Bold.otf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "C:/Windows/Fonts/segoeuib.ttf",
    "/System/Library/Fonts/Helvetica.ttc",
]


def load_font(size):
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default(size=size)


def gradient(size, top, bottom):
    w, h = size
    base = Image.new("RGB", size, top)
    px = base.load()
    for y in range(h):
        t = y / max(h - 1, 1)
        row = tuple(int(top[k] * (1 - t) + bottom[k] * t) for k in range(3))
        for x in range(w):
            px[x, y] = row
    return base


def chip_glyph(draw, cx, cy, size):
    """A microchip: rounded body, pins on all sides, a die in the middle."""
    body = size * 0.56
    half = body / 2
    pin_len = size * 0.14
    pin_w = size * 0.05
    pin_gap = body / 4
    pin_color = (150, 200, 255)

    for i in (-1, 0, 1):
        offset = i * pin_gap
        # top, bottom
        draw.rounded_rectangle([cx + offset - pin_w, cy - half - pin_len, cx + offset + pin_w, cy - half + 2], pin_w, fill=pin_color)
        draw.rounded_rectangle([cx + offset - pin_w, cy + half - 2, cx + offset + pin_w, cy + half + pin_len], pin_w, fill=pin_color)
        # left, right
        draw.rounded_rectangle([cx - half - pin_len, cy + offset - pin_w, cx - half + 2, cy + offset + pin_w], pin_w, fill=pin_color)
        draw.rounded_rectangle([cx + half - 2, cy + offset - pin_w, cx + half + pin_len, cy + offset + pin_w], pin_w, fill=pin_color)

    draw.rounded_rectangle([cx - half, cy - half, cx + half, cy + half], size * 0.09, fill=(236, 244, 255))
    die = half * 0.52
    draw.rounded_rectangle([cx - die, cy - die, cx + die, cy + die], size * 0.045, fill=(34, 86, 190))
    inner = die * 0.5
    draw.rounded_rectangle([cx - inner, cy - inner, cx + inner, cy + inner], size * 0.02, fill=(120, 190, 255))


def make_icon(px):
    s = px * SCALE
    img = gradient((s, s), (24, 40, 92), (10, 14, 34))
    draw = ImageDraw.Draw(img)
    chip_glyph(draw, s / 2, s / 2, s * 0.86)
    # Rounded-square mask.
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, s - 1, s - 1], s * 0.22, fill=255)
    out = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out.resize((px, px), Image.LANCZOS)


def make_banner(width, height):
    w, h = width * SCALE, height * SCALE
    img = gradient((w, h), (22, 36, 86), (8, 12, 30))
    draw = ImageDraw.Draw(img)
    chip_size = h * 0.56
    font = load_font(int(h * 0.19))
    text = "ChipOS"
    box = draw.textbbox((0, 0), text, font=font)
    text_w, text_h = box[2] - box[0], box[3] - box[1]
    chip_w = chip_size * 0.56 + chip_size * 0.28  # body plus pins
    gap = h * 0.10
    start = (w - (chip_w + gap + text_w)) / 2
    chip_cx = start + chip_w / 2
    chip_glyph(draw, chip_cx, h * 0.5, chip_size)
    draw.text((start + chip_w + gap - box[0], (h - text_h) / 2 - box[1]), text, font=font, fill=(240, 246, 255))
    return img.resize((width, height), Image.LANCZOS)


def save(img, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print("wrote", os.path.normpath(path))


if __name__ == "__main__":
    save(make_icon(144), "mipmap-xxhdpi/ic_launcher.png")
    save(make_icon(96), "mipmap-xhdpi/ic_launcher.png")
    save(make_banner(640, 360), "drawable-xhdpi/banner.png")
