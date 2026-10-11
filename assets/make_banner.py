#!/usr/bin/env python3
"""Render assets/banner.png.

Same layout and palette as the Margy banner: mint field, white glyph on the
left inside a faint rounded outline, wordmark and subtitle on the right.
The glyph is a beamed note rather than a paper plane -- same family, different
app.
"""

import pathlib

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).parent

W, H = 900, 260
MINT = (0x8D, 0xD1, 0xB0)
INK = (0x1C, 0x2C, 0x24)
INK_SOFT = (0x30, 0x4A, 0x3C)
WHITE = (255, 255, 255)

BOLD = "/usr/share/fonts/noto/NotoSans-Bold.ttf"
REGULAR = "/usr/share/fonts/noto/NotoSans-Regular.ttf"

WORDMARK = "MargyT"
SUBTITLE = "a TikTok mod for Android"


def note(draw, cx, cy, scale):
    """Beamed eighth note, same geometry as the launcher icon."""

    def p(x, y):
        return (cx + (x - 54) * scale, cy + (y - 53) * scale)

    for hx, hy in ((37, 76), (71, 68)):
        draw.ellipse([p(hx - 11, hy - 8), p(hx + 11, hy + 8)], fill=WHITE)
    draw.polygon([p(42, 30), p(48, 30), p(48, 76), p(42, 76)], fill=WHITE)
    draw.polygon([p(76, 22), p(82, 22), p(82, 68), p(76, 68)], fill=WHITE)
    draw.polygon([p(42, 30), p(82, 22), p(82, 34), p(42, 42)], fill=WHITE)


def main():
    im = Image.new("RGB", (W, H), MINT)
    d = ImageDraw.Draw(im)

    # faint rounded outline around the glyph, as on the Margy banner
    d.rounded_rectangle(
        [65, 32, 250, 217], radius=58, outline=(0x9E, 0xD8, 0xBC), width=2
    )
    note(d, 157, 124, 0.95)

    wordmark = ImageFont.truetype(BOLD, 82)
    subtitle = ImageFont.truetype(REGULAR, 30)

    d.text((285, 74), WORDMARK, font=wordmark, fill=INK)
    d.text((288, 168), SUBTITLE, font=subtitle, fill=INK_SOFT)

    out = HERE / "banner.png"
    im.save(out)
    print("wrote", out, im.size)


if __name__ == "__main__":
    main()
