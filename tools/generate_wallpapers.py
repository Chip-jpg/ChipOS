#!/usr/bin/env python3
"""
Generates the bundled ChipOS wallpapers: procedural, layered nature landscapes.

Everything is drawn from code (numpy + Pillow), so the images carry no third-party
copyright and the repo stays small. Run it again to regenerate them:

    pip install pillow numpy
    python tools/generate_wallpapers.py            # writes into app/src/main/assets/wallpapers
    python tools/generate_wallpapers.py ./out      # or into another folder

Want real photos instead? Delete these files and drop your own JPG/PNG/WebP files
(ideally 1920x1080) into app/src/main/assets/wallpapers/. The app picks up whatever is there.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

W, H = 1920, 1080
DEFAULT_OUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "wallpapers"
)


def hexc(value):
    value = value.lstrip("#")
    return np.array([int(value[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32) / 255.0


def vgrad(stops):
    """Vertical gradient, stops = [(position 0..1, '#rrggbb'), ...]. Returns H x W x 3."""
    ys = np.linspace(0, 1, H, dtype=np.float32)
    pos = np.array([p for p, _ in stops], dtype=np.float32)
    cols = np.stack([hexc(c) for _, c in stops])
    column = np.stack([np.interp(ys, pos, cols[:, k]) for k in range(3)], axis=1)
    return np.repeat(column[:, None, :], W, axis=1).astype(np.float32)


def noise1d(rng, octaves, rough, smooth, base_pts=3):
    """1-D fractal noise in [0, 1], length W. smooth=False gives angular mountain ridges."""
    x = np.linspace(0, 1, W)
    total = np.zeros(W)
    amp = 1.0
    for octave in range(octaves):
        pts = base_pts * (2 ** octave)
        values = rng.normal(0, 1, pts + 2)
        pos = x * pts
        idx = np.floor(pos).astype(int)
        frac = pos - idx
        if smooth:
            frac = frac * frac * (3 - 2 * frac)
        total += (values[idx] * (1 - frac) + values[idx + 1] * frac) * amp
        amp *= rough
    total -= total.min()
    total /= max(total.max(), 1e-6)
    return total


def add_layer(img, ridge_y, color, haze_color, haze):
    """Fill everything below ridge_y (a W-long array of row positions) with a hazy colour."""
    ys = np.arange(H, dtype=np.float32)[:, None]
    mask = np.clip(ys - ridge_y[None, :] + 0.5, 0, 1)
    shade = 1.0 - 0.28 * np.clip((ys - ridge_y[None, :]) / 420.0, 0, 1)
    col = color * (1 - haze) + haze_color * haze
    layer = col[None, None, :] * shade[..., None]
    img[:] = img * (1 - mask[..., None]) + layer * mask[..., None]


def mountains(img, rng, n, far, near, haze_color, base0, base1, amp0, amp1,
              haze_strength=0.85, smooth=False, rough=0.52, octaves=8):
    far, near = hexc(far), hexc(near)
    for i in range(n):
        t = i / max(n - 1, 1)
        base = (base0 + (base1 - base0) * t) * H
        amp = (amp0 + (amp1 - amp0) * t) * H
        ridge = base - amp * noise1d(rng, octaves, rough, smooth)
        ridge = np.convolve(np.pad(ridge, 3, mode='edge'), np.ones(7) / 7, mode='valid')
        add_layer(img, ridge, far * (1 - t) + near * t, hexc(haze_color) if isinstance(haze_color, str) else haze_color,
                  haze_strength * (1 - t) ** 1.6)


def glow(img, cx, cy, radius, color, strength):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    d2 = ((xx - cx * W) ** 2 + (yy - cy * H) ** 2) / (radius * W) ** 2
    img += (np.exp(-d2) * strength)[..., None] * hexc(color)[None, None, :]


def disc(img, cx, cy, radius_px, color):
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    d = np.sqrt((xx - cx * W) ** 2 + (yy - cy * H) ** 2)
    mask = np.clip(radius_px - d + 0.5, 0, 1)
    img[:] = img * (1 - mask[..., None]) + hexc(color)[None, None, :] * mask[..., None]


def stars(img, rng, horizon, count=1800):
    xs = rng.integers(0, W, count)
    ys = (rng.random(count) ** 1.4 * horizon * H).astype(int)
    bright = rng.random(count) ** 3 * 0.9 + 0.1
    for x, y, b in zip(xs, ys, bright):
        img[y, x] += b
        if b > 0.7:
            img[max(y - 1, 0):y + 2, max(x - 1, 0):x + 2] += b * 0.25


def aurora(img, rng):
    layer = np.zeros((H, W, 3), dtype=np.float32)
    ys = np.arange(H, dtype=np.float32)[:, None]
    for k, color in enumerate(["#2cff9a", "#3be6c8", "#8a5cff"]):
        center = (0.22 + 0.07 * k) * H + 90 * np.sin(np.linspace(0, 5 + k, W) + rng.random() * 6)
        center += 60 * noise1d(rng, 4, 0.5, True)
        sigma = (70 + 25 * noise1d(rng, 3, 0.5, True)) * (1 + 0.3 * k)
        strength = 0.35 + 0.65 * noise1d(rng, 5, 0.55, True)
        band = np.exp(-(((ys - center[None, :]) / sigma[None, :]) ** 2)) * strength[None, :]
        layer += band[..., None] * hexc(color)[None, None, :] * (0.55 - 0.1 * k)
    pil = Image.fromarray((np.clip(layer, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(14))
    img += np.asarray(pil, dtype=np.float32) / 255.0 * 1.15


def reflect(img, rng, horizon, tint, tint_amount=0.35, darken=0.72):
    """Replace everything below the horizon with a rippled mirror image of what is above."""
    h0 = int(horizon * H)
    src = img.copy()
    rows = np.arange(h0, H)
    mirror = np.clip(2 * h0 - rows, 0, H - 1)
    depth = (rows - h0) / max(H - h0, 1)
    shift = (np.sin(rows * 0.06 + rng.random() * 6) * (1 + 9 * depth ** 1.3)).astype(int)
    xs = (np.arange(W)[None, :] + shift[:, None]) % W
    water = src[mirror[:, None], xs]
    water = water * darken * (1 - tint_amount) + hexc(tint)[None, None, :] * tint_amount
    # Soft horizontal streaks make it read as water.
    streak = (0.5 + 0.5 * np.sin(rows[:, None] * 0.9 + rng.random(W)[None, :] * 3)) * 0.05 * depth[:, None]
    water = water + streak[..., None]
    pil = Image.fromarray((np.clip(water, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.6))
    img[h0:] = np.asarray(pil, dtype=np.float32) / 255.0


def finish(img, rng, path):
    # Subtle vignette and film grain; the grain also hides gradient banding in JPEG.
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    r2 = ((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2
    img *= (1 - 0.18 * np.clip(r2, 0, 2))[..., None]
    img += rng.normal(0, 0.011, (H, W))[..., None]
    out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    out.save(path, quality=88, optimize=True, progressive=True)
    print(f"  {os.path.basename(path)}  {os.path.getsize(path) / 1024:.0f} KB")


# --- scenes -----------------------------------------------------------------------------

def alpine_dawn(rng):
    img = vgrad([(0, "#2a3b6e"), (0.42, "#b9729a"), (0.7, "#f6ae7c"), (1, "#ffe0a8")])
    glow(img, 0.68, 0.6, 0.30, "#ffc27a", 0.55)
    mountains(img, rng, 6, "#a393c0", "#161d33", "#f2b48a", 0.60, 1.0, 0.16, 0.34)
    return img


def dusk_lake(rng):
    img = vgrad([(0, "#1a1946"), (0.35, "#5d3a85"), (0.62, "#e0735f"), (0.72, "#ffb36b"), (1, "#ffb36b")])
    glow(img, 0.5, 0.64, 0.28, "#ff9a5c", 0.5)
    mountains(img, rng, 5, "#6d4a8e", "#1d1a3a", "#e98a6a", 0.52, 0.66, 0.14, 0.26, rough=0.5)
    reflect(img, rng, 0.66, "#3a2a5e", 0.30)
    # Dark shoreline in the foreground.
    shore = 0.93 * H - 0.07 * H * noise1d(rng, 6, 0.55, True)
    add_layer(img, shore, hexc("#0c0b1d"), hexc("#0c0b1d"), 0.0)
    return img


def forest_mist(rng):
    img = vgrad([(0, "#8fa8ad"), (0.5, "#cfdcd8"), (1, "#e6eee9")])
    glow(img, 0.3, 0.35, 0.35, "#ffffff", 0.35)
    mountains(img, rng, 7, "#b3c7bf", "#0f261d", "#e2ebe6", 0.50, 1.02, 0.12, 0.26,
              haze_strength=0.9, smooth=True, rough=0.48, octaves=6)
    mist = np.exp(-(((np.arange(H)[:, None] - 0.72 * H) / (0.07 * H)) ** 2)).astype(np.float32)
    img += mist[..., None] * 0.18
    return img


def desert_dunes(rng):
    img = vgrad([(0, "#e98d5b"), (0.4, "#f6c58e"), (0.7, "#ffe9c4"), (1, "#ffe9c4")])
    disc(img, 0.72, 0.30, 70, "#fff3d6")
    glow(img, 0.72, 0.30, 0.22, "#ffd9a0", 0.6)
    mountains(img, rng, 6, "#e8b27a", "#5e2f1d", "#ffe3b8", 0.56, 1.02, 0.07, 0.18,
              haze_strength=0.7, smooth=True, rough=0.35, octaves=4)
    return img


def night_aurora(rng):
    img = vgrad([(0, "#03060f"), (0.5, "#0a1a35"), (1, "#15385a")])
    stars(img, rng, 0.75)
    aurora(img, rng)
    mountains(img, rng, 5, "#17304d", "#03050b", "#1a4a68", 0.66, 1.0, 0.10, 0.26,
              haze_strength=0.75, rough=0.5)
    return img


def ocean_sunset(rng):
    img = vgrad([(0, "#1b2a5c"), (0.3, "#6d4a8f"), (0.52, "#f2706a"), (0.58, "#ffc27a"), (1, "#ffc27a")])
    glow(img, 0.5, 0.58, 0.30, "#ff9d5c", 0.6)
    disc(img, 0.5, 0.555, 58, "#fff0cf")
    h0 = int(0.58 * H)
    rows = np.arange(h0, H, dtype=np.float32)
    depth = ((rows - h0) / (H - h0))[:, None]
    sea = np.stack([np.interp(depth[:, 0], [0, 1], [hexc("#6d3f6a")[k], hexc("#0b1230")[k]]) for k in range(3)], axis=1)
    img[h0:] = np.repeat(sea[:, None, :], W, axis=1)
    xs = np.arange(W, dtype=np.float32)[None, :]
    width = 30 + 260 * depth
    wobble = 0.6 + 0.4 * np.sin(rows[:, None] * 0.3 + rng.random(W)[None, :] * 1.5)
    streak = np.exp(-(((xs - 0.5 * W) / width) ** 2)) * wobble * (1 - 0.55 * depth)
    img[h0:] += streak[..., None] * hexc("#ffd49a")[None, None, :] * 0.9
    sea_pil = Image.fromarray((np.clip(img[h0:], 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.4))
    img[h0:] = np.asarray(sea_pil, dtype=np.float32) / 255.0
    return img


def glacier_blue(rng):
    img = vgrad([(0, "#7fb4de"), (0.5, "#cfe6f4"), (1, "#eef7fc")])
    glow(img, 0.25, 0.3, 0.3, "#ffffff", 0.45)
    mountains(img, rng, 6, "#d6e9f5", "#27425f", "#eef7fc", 0.58, 1.0, 0.20, 0.40,
              haze_strength=0.9, rough=0.56)
    return img


def autumn_valley(rng):
    img = vgrad([(0, "#e08a6a"), (0.45, "#f6b98a"), (1, "#fde0b4")])
    disc(img, 0.22, 0.34, 54, "#fff1d0")
    glow(img, 0.22, 0.34, 0.26, "#ffd2a0", 0.55)
    mountains(img, rng, 7, "#e0a47c", "#3d150e", "#fcd9ae", 0.54, 1.03, 0.10, 0.24,
              haze_strength=0.85, smooth=True, rough=0.5, octaves=6)
    return img


SCENES = [
    ("alpine-dawn", alpine_dawn, 11),
    ("dusk-lake", dusk_lake, 23),
    ("forest-mist", forest_mist, 37),
    ("desert-dunes", desert_dunes, 41),
    ("night-aurora", night_aurora, 53),
    ("ocean-sunset", ocean_sunset, 67),
    ("glacier-blue", glacier_blue, 71),
    ("autumn-valley", autumn_valley, 83),
]


def main():
    out_dir = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_OUT
    os.makedirs(out_dir, exist_ok=True)
    print(f"Writing wallpapers to {os.path.abspath(out_dir)}")
    for name, build, seed in SCENES:
        rng = np.random.default_rng(seed)
        img = build(rng)
        finish(img, np.random.default_rng(seed + 1000), os.path.join(out_dir, f"{name}.jpg"))


if __name__ == "__main__":
    main()
