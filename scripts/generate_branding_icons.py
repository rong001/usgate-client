#!/usr/bin/env python3
"""Generate USGate launcher + notification PNG assets (blue/indigo shield-gate)."""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
PREVIEWS = ROOT / "branding-previews"

# Brand palette
INDIGO_900 = (49, 46, 129)       # #312E81
INDIGO_700 = (67, 56, 202)       # #4338CA
INDIGO_500 = (99, 102, 241)      # #6366F1
INDIGO_300 = (165, 180, 252)     # #A5B4FC
BLUE_400 = (96, 165, 250)        # #60A5FA
WHITE = (255, 255, 255, 255)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_gradient_bg(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        t = y / max(size - 1, 1)
        c = lerp(INDIGO_700, INDIGO_900, t)
        for x in range(size):
            # subtle radial brightening toward center
            dx = (x - size / 2) / (size / 2)
            dy = (y - size / 2) / (size / 2)
            r = math.sqrt(dx * dx + dy * dy)
            boost = max(0.0, 1.0 - r) * 0.18
            col = tuple(min(255, int(c[i] + (255 - c[i]) * boost)) for i in range(3)) + (255,)
            px[x, y] = col
    return img


def shield_path(cx, cy, w, h):
    """Return polygon points for a classic shield."""
    left, right = cx - w / 2, cx + w / 2
    top = cy - h / 2
    bottom = cy + h / 2
    mid_y = top + h * 0.55
    return [
        (cx, top),
        (right, top + h * 0.12),
        (right, mid_y),
        (cx, bottom),
        (left, mid_y),
        (left, top + h * 0.12),
    ]


def draw_foreground(size: int, pad_ratio: float = 0.22) -> Image.Image:
    """Transparent foreground with shield + gate motif (safe for adaptive icon)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx = cy = size / 2
    # Keep motif inside ~66% safe zone
    usable = size * (1 - 2 * pad_ratio)
    sw, sh = usable * 0.78, usable * 0.92

    # Soft glow disc
    glow_r = usable * 0.48
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse(
        [cx - glow_r, cy - glow_r, cx + glow_r, cy + glow_r],
        fill=(165, 180, 252, 55),
    )
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    # Outer shield (white)
    outer = shield_path(cx, cy, sw, sh)
    draw.polygon(outer, fill=WHITE)

    # Inner indigo shield inset
    inset = size * 0.035
    inner = shield_path(cx, cy + inset * 0.15, sw - inset * 2.2, sh - inset * 2.4)
    draw.polygon(inner, fill=INDIGO_500 + (255,))

    # Gate / portal: rounded arch cutout (lighter)
    gate_w = sw * 0.38
    gate_h = sh * 0.42
    gate_left = cx - gate_w / 2
    gate_right = cx + gate_w / 2
    gate_top = cy - gate_h * 0.15
    gate_bottom = cy + sh * 0.28

    # Arch as pie + rectangle
    arch_bbox = [
        gate_left,
        gate_top - gate_w / 2,
        gate_right,
        gate_top + gate_w / 2,
    ]
    draw.pieslice(arch_bbox, 180, 360, fill=INDIGO_300 + (255,))
    draw.rectangle(
        [gate_left, gate_top, gate_right, gate_bottom],
        fill=INDIGO_300 + (255,),
    )

    # Inner gate darker opening
    iw = gate_w * 0.55
    ih_top = gate_top + gate_w * 0.08
    draw.pieslice(
        [cx - iw / 2, ih_top - iw / 2, cx + iw / 2, ih_top + iw / 2],
        180,
        360,
        fill=INDIGO_900 + (255,),
    )
    draw.rectangle(
        [cx - iw / 2, ih_top, cx + iw / 2, gate_bottom - size * 0.02],
        fill=INDIGO_900 + (255,),
    )

    # Keyhole accent
    kr = size * 0.028
    kx, ky = cx, cy + sh * 0.02
    draw.ellipse([kx - kr, ky - kr, kx + kr, ky + kr], fill=WHITE)
    draw.polygon(
        [
            (kx - kr * 0.55, ky + kr * 0.4),
            (kx + kr * 0.55, ky + kr * 0.4),
            (kx + kr * 0.35, ky + kr * 2.2),
            (kx - kr * 0.35, ky + kr * 2.2),
        ],
        fill=WHITE,
    )

    # Top highlight bar (gate lintel)
    bar_y = gate_top - gate_w * 0.05
    draw.rounded_rectangle(
        [cx - gate_w * 0.55, bar_y - size * 0.012, cx + gate_w * 0.55, bar_y + size * 0.012],
        radius=size * 0.01,
        fill=BLUE_400 + (255,),
    )

    return img


def compose_full(size: int) -> Image.Image:
    bg = draw_gradient_bg(size)
    fg = draw_foreground(size, pad_ratio=0.18)
    return Image.alpha_composite(bg, fg)


def draw_notification_icon(size: int) -> Image.Image:
    """White alpha silhouette for status bar."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx = cy = size / 2
    sw, sh = size * 0.62, size * 0.76
    outer = shield_path(cx, cy, sw, sh)
    draw.polygon(outer, fill=WHITE)

    # Cut gate (transparent) by punching with destination-out via mask
    mask = Image.new("L", (size, size), 0)
    mdraw = ImageDraw.Draw(mask)
    gate_w = sw * 0.36
    gate_top = cy - sh * 0.05
    gate_bottom = cy + sh * 0.28
    mdraw.pieslice(
        [cx - gate_w / 2, gate_top - gate_w / 2, cx + gate_w / 2, gate_top + gate_w / 2],
        180,
        360,
        fill=255,
    )
    mdraw.rectangle([cx - gate_w / 2, gate_top, cx + gate_w / 2, gate_bottom], fill=255)
    # Apply hole
    pixels = img.load()
    mp = mask.load()
    for y in range(size):
        for x in range(size):
            if mp[x, y] > 128:
                pixels[x, y] = (0, 0, 0, 0)
    return img


def save_mipmaps(full: Image.Image):
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, px in densities.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        icon = full.resize((px, px), Image.Resampling.LANCZOS)
        icon.save(out_dir / "ic_launcher.png", "PNG")
        icon.save(out_dir / "ic_launcher_round.png", "PNG")
        print(f"wrote {folder}/ic_launcher.png ({px}px)")


def main():
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    master = 1024
    full = compose_full(master)
    fg = draw_foreground(master, pad_ratio=0.22)
    bg = draw_gradient_bg(master)

    full.save(PREVIEWS / "launcher_full.png")
    fg.save(PREVIEWS / "launcher_foreground.png")
    bg.save(PREVIEWS / "launcher_background.png")

    # Adaptive layers at 108dp * 4 = 432 for xxxhdpi-ish source; also store drawable PNGs
    draw_dir = RES / "drawable"
    draw_dir.mkdir(parents=True, exist_ok=True)
    # Keep vector XML for adaptive; also write PNG fallbacks used if needed
    fg.resize((432, 432), Image.Resampling.LANCZOS).save(draw_dir / "ic_launcher_foreground.png")
    bg.resize((432, 432), Image.Resampling.LANCZOS).save(draw_dir / "ic_launcher_background.png")

    save_mipmaps(full)

    # Notification icons (mdpi..xxxhdpi as drawable densities via drawable-*dpi)
    notif_sizes = {
        "drawable-mdpi": 24,
        "drawable-hdpi": 36,
        "drawable-xhdpi": 48,
        "drawable-xxhdpi": 72,
        "drawable-xxxhdpi": 96,
    }
    notif_master = draw_notification_icon(256)
    notif_master.save(PREVIEWS / "ic_stat_vpn.png")
    for folder, px in notif_sizes.items():
        d = RES / folder
        d.mkdir(parents=True, exist_ok=True)
        notif_master.resize((px, px), Image.Resampling.LANCZOS).save(d / "ic_stat_vpn.png")
        print(f"wrote {folder}/ic_stat_vpn.png ({px}px)")

    # Landing logo preview
    logo = compose_full(256)
    logo.save(PREVIEWS / "logo_256.png")
    # Also copy into landing for the page
    (ROOT / "landing").mkdir(exist_ok=True)
    logo.save(ROOT / "landing" / "usgate-logo.png")
    print("done")


if __name__ == "__main__":
    main()
