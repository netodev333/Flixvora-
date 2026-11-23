#!/usr/bin/env python3
"""Offline generator for the Flixvora launcher icon and in-app logo.

Renders a cinematic play-mark on an obsidian radial background. No network,
no external assets: every pixel is drawn here with Pillow.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFilter

RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

SS = 4  # supersample factor

# Brand palette (kept in sync with ui/theme/Color.kt)
OBSIDIAN_INNER = (26, 27, 46)
OBSIDIAN_OUTER = (8, 8, 14)
CRIMSON = (229, 9, 20)
CORAL = (255, 59, 78)
WHITE = (255, 255, 255)


def radial_background(size, inner, outer):
    img = Image.new("RGB", (size, size), outer)
    px = img.load()
    c = (size - 1) / 2.0
    maxd = math.hypot(c, c)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / maxd
            d = min(1.0, d)
            t = d ** 1.25
            px[x, y] = (
                int(inner[0] + (outer[0] - inner[0]) * t),
                int(inner[1] + (outer[1] - inner[1]) * t),
                int(inner[2] + (outer[2] - inner[2]) * t),
            )
    return img


def linear_gradient(size, top, bottom):
    img = Image.new("RGB", (size, size))
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        row = (
            int(top[0] + (bottom[0] - top[0]) * t),
            int(top[1] + (bottom[1] - top[1]) * t),
            int(top[2] + (bottom[2] - top[2]) * t),
        )
        for x in range(size):
            px[x, y] = row
    return img


def rounded_triangle_mask(size, inset, radius_ratio=0.16):
    """A play triangle pointing right, with rounded corners."""
    x0 = inset
    x1 = size - inset
    top = inset
    bot = size - inset
    cy = (top + bot) / 2.0
    v1 = (x0, top)
    v2 = (x0, bot)
    v3 = (x1, cy)
    r = int((x1 - x0) * radius_ratio)

    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.polygon([v1, v2, v3], fill=255)
    for v in (v1, v2, v3):
        d.ellipse([v[0] - r, v[1] - r, v[0] + r, v[1] + r], fill=255)
    return mask


def glyph(size, inset_ratio):
    """RGBA glyph layer (glowing play-mark) on a transparent canvas."""
    inset = int(size * inset_ratio)
    mask = rounded_triangle_mask(size, inset)
    grad = linear_gradient(size, CORAL, CRIMSON)

    glow_mask = mask.filter(ImageFilter.GaussianBlur(size * 0.045))
    glow_col = Image.new("RGBA", (size, size), CRIMSON + (0,))
    glow_col.putalpha(glow_mask.point(lambda a: int(a * 0.55)))
    glow = glow_col

    body = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    body.paste(grad, (0, 0), mask)

    return Image.alpha_composite(glow, body)


def compose_legacy(size, shape):
    """Flattened launcher icon for pre-adaptive devices."""
    bg = radial_background(size, OBSIDIAN_INNER, OBSIDIAN_OUTER).convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    if shape == "circle":
        d.ellipse([0, 0, size - 1, size - 1], fill=255)
    else:
        r = int(size * 0.22)
        d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
    bg.putalpha(mask)
    return Image.alpha_composite(bg, glyph(size, 0.28))


def compose_foreground(size):
    return glyph(size, 0.28)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG", optimize=True)
    print("wrote", path, img.size)


def main():
    densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    for dens, scale in densities.items():
        legacy = int(48 * scale)
        fg = int(108 * scale)
        out_dir = os.path.join(RES, f"mipmap-{dens}")
        save(
            compose_legacy(legacy * SS, "rounded").resize((legacy, legacy), Image.LANCZOS),
            os.path.join(out_dir, "ic_launcher.png"),
        )
        save(
            compose_legacy(legacy * SS, "circle").resize((legacy, legacy), Image.LANCZOS),
            os.path.join(out_dir, "ic_launcher_round.png"),
        )
        save(
            compose_foreground(fg * SS).resize((fg, fg), Image.LANCZOS),
            os.path.join(out_dir, "ic_launcher_fg.png"),
        )

    popup = 512
    save(
        compose_legacy(popup * 2, "circle").resize((popup, popup), Image.LANCZOS),
        os.path.join(RES, "drawable-nodpi", "ic_popup_logo.png"),
    )
    save(
        compose_legacy(popup * 2, "rounded").resize((popup, popup), Image.LANCZOS),
        os.path.join(RES, "drawable-nodpi", "ic_launcher_fg.png"),
    )


if __name__ == "__main__":
    main()
