#!/usr/bin/env python3
"""Generate launcher icons for Galaxy Diablo."""
import os
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def make_gradient(size):
    # neon blue glassmorphism: deep navy -> royal blue -> neon cyan
    img = Image.new("RGB", (size, size), (3, 7, 18))
    draw = ImageDraw.Draw(img)
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size)
            r = int(3 + (34 - 3) * t)
            g = int(7 + (211 - 7) * t)
            b = int(18 + (238 - 18) * t)
            draw.point((x, y), fill=(r, g, b))
    return img

def draw_star(draw, cx, cy, r_outer, r_inner, fill):
    import math
    points = []
    for i in range(10):
        angle = -math.pi / 2 + i * math.pi / 5
        r = r_outer if i % 2 == 0 else r_inner
        points.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    draw.polygon(points, fill=fill)

def make_icon(size, round_mask=False):
    img = make_gradient(size)
    draw = ImageDraw.Draw(img, "RGBA")
    # ring
    pad = int(size * 0.08)
    draw.ellipse((pad, pad, size - pad, size - pad), outline=(255, 255, 255, 90), width=max(1, size // 64))
    # star
    cx, cy = size // 2, size // 2
    r_outer = int(size * 0.30)
    r_inner = int(size * 0.13)
    draw_star(draw, cx, cy, r_outer, r_inner, (255, 255, 255, 245))
    # small stars
    for (fx, fy, fr) in [(0.20, 0.78, 0.02), (0.80, 0.22, 0.018), (0.78, 0.80, 0.020), (0.22, 0.20, 0.015)]:
        px, py = int(fx * size), int(fy * size)
        rr = int(fr * size)
        draw.ellipse((px - rr, py - rr, px + rr, py + rr), fill=(255, 255, 255, 200))
    if round_mask:
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size, size), fill=255)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(img, (0, 0), mask)
        return out
    return img.convert("RGBA")

def main():
    for folder, size in SIZES.items():
        out_dir = os.path.join(BASE, folder)
        os.makedirs(out_dir, exist_ok=True)
        make_icon(size, round_mask=False).save(os.path.join(out_dir, "ic_launcher.png"))
        make_icon(size, round_mask=True).save(os.path.join(out_dir, "ic_launcher_round.png"))
        print(f"Wrote {folder} ({size}x{size})")

if __name__ == "__main__":
    main()
