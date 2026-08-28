"""Generates the Reevz Mealz launcher icon: a pixel-art chicken in the app's retro palette.

Run from anywhere:  python tools/make_icon.py

Writes the adaptive-icon vector layers plus legacy PNG mipmaps for every density bucket.
The icon is generated rather than hand-drawn so the art stays editable: change the ART grid
below and every density bucket and both adaptive layers are rebuilt in step. Editing the PNGs
by hand would leave them disagreeing with the vectors.
Pure stdlib - the PNG encoder is hand-rolled so this needs no Pillow.
"""
import os
import struct
import zlib

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res")

# The chicken, 16x16. Front-facing and blocky, in the app's own colours.
ART = [
    "................",
    ".......RR.......",
    "......RRRR......",
    ".....WWWWWW.....",
    ".....WEWWEW.....",
    ".....WWWWWW.....",
    "....WWWYYWWW....",
    "....WWWWWWWW....",
    "...WWWWWWWWWW...",
    "..WGWWWWWWWWGW..",
    "..WGWWWWWWWWGW..",
    "..WWWWWWWWWWWW..",
    "...WWWWWWWWWW...",
    "....WWWWWWWW....",
    "......Y..Y......",
    ".....YY..YY.....",
]

PALETTE = {
    "W": (0xFF, 0xFF, 0xFF),  # body
    "G": (0xC3, 0xBB, 0xEA),  # wing shading
    "R": (0xFF, 0x5F, 0xA2),  # comb - retro pink
    "Y": (0xFF, 0xD0, 0x28),  # beak and legs - arcade yellow
    "E": (0x16, 0x13, 0x2E),  # eye
}
BACKGROUND = (0x1E, 0x1A, 0x3C)  # the app's night indigo

GRID = len(ART)
assert all(len(row) == GRID for row in ART), "art must be square"


def hex_of(rgb):
    return "#FF%02X%02X%02X" % rgb


def runs_for(colors):
    """Merge each row into horizontal runs, so the vector is a few paths not 256 squares."""
    out = {}
    for y, row in enumerate(ART):
        x = 0
        while x < GRID:
            key = row[x]
            if key not in colors:
                x += 1
                continue
            start = x
            while x < GRID and row[x] == key:
                x += 1
            out.setdefault(key, []).append((start, y, x - start))
    return out


def art_bounds():
    xs = [x for y, row in enumerate(ART) for x, c in enumerate(row) if c != "."]
    ys = [y for y, row in enumerate(ART) for c in row if c != "."]
    return min(xs), min(ys), max(xs) + 1, max(ys) + 1


def safe_scale():
    """Largest scale that keeps every drawn pixel inside the 72dp safe *circle*.

    Sizing to the 72dp safe square is not enough: a circular mask (Pixel's default, and the
    themed-icon shape) inscribes a circle in that square, so anything near the top or bottom
    centre - the comb and the feet - would be sliced off. Fitting the art's furthest corner to
    the circle's radius is what guarantees the whole bird survives every mask.
    """
    x0, y0, x1, y1 = art_bounds()
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    furthest = 0.0
    for y, row in enumerate(ART):
        for x, key in enumerate(row):
            if key == ".":
                continue
            for corner_x in (x, x + 1):
                for corner_y in (y, y + 1):
                    furthest = max(furthest, ((corner_x - cx) ** 2 + (corner_y - cy) ** 2) ** 0.5)
    return 36.0 / furthest, cx, cy


def vector(colors, monochrome=False):
    """An adaptive-icon layer: the art centred on the 108dp canvas, sized to the safe circle."""
    scale, cx, cy = safe_scale()
    off_x = 54.0 - cx * scale
    off_y = 54.0 - cy * scale

    lines = [
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        '    android:viewportWidth="108"',
        '    android:viewportHeight="108">',
    ]
    for key, cells in runs_for(colors).items():
        data = []
        for run_x, run_y, width in cells:
            px = off_x + run_x * scale
            py = off_y + run_y * scale
            data.append("M%.2f,%.2fh%.2fv%.2fh-%.2fz" % (px, py, width * scale, scale, width * scale))
        fill = "#FF000000" if monochrome else hex_of(PALETTE[key])
        lines.append("    <path")
        lines.append('        android:fillColor="%s"' % fill)
        lines.append('        android:pathData="%s" />' % "".join(data))
    lines.append("</vector>")
    return "\n".join(lines) + "\n"


def solid_background():
    return (
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
        "    <path\n"
        '        android:fillColor="%s"\n'
        '        android:pathData="M0,0h108v108h-108z" />\n'
        "</vector>\n" % hex_of(BACKGROUND)
    )


def write_png(path, size, round_mask):
    """Legacy mipmap. One virtual cell of margin all round so nothing touches the edge."""
    virtual = GRID + 2
    rows = []
    centre = (size - 1) / 2.0
    radius = size / 2.0
    for py in range(size):
        row = bytearray()
        for px in range(size):
            if round_mask:
                dx, dy = px - centre, py - centre
                inside = (dx * dx + dy * dy) <= radius * radius
            else:
                # Rounded square, not a bare rectangle: lint's IconLauncherShape wants a
                # launcher shape here, and pre-Oreo launchers do not mask the icon themselves.
                corner = size * 0.22
                cx = min(max(px, corner), size - 1 - corner)
                cy = min(max(py, corner), size - 1 - corner)
                dx, dy = px - cx, py - cy
                inside = (dx * dx + dy * dy) <= corner * corner
            if not inside:
                row += bytes((0, 0, 0, 0))
                continue
            vx = px * virtual // size - 1
            vy = py * virtual // size - 1
            key = ART[vy][vx] if 0 <= vx < GRID and 0 <= vy < GRID else "."
            rgb = PALETTE.get(key, BACKGROUND)
            row += bytes((rgb[0], rgb[1], rgb[2], 255))
        rows.append(row)

    raw = b"".join(b"\x00" + bytes(r) for r in rows)

    def chunk(tag, payload):
        return (
            struct.pack(">I", len(payload))
            + tag
            + payload
            + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)
        )

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)


def main():
    drawable = os.path.join(RES, "drawable")
    with open(os.path.join(drawable, "ic_launcher_background.xml"), "w") as f:
        f.write(solid_background())
    with open(os.path.join(drawable, "ic_launcher_foreground.xml"), "w") as f:
        f.write(vector(set(PALETTE)))
    # Themed icons are tinted a single colour, so the eyes are left as holes rather
    # than filled - otherwise the whole bird flattens into one featureless blob.
    with open(os.path.join(drawable, "ic_launcher_monochrome.xml"), "w") as f:
        f.write(vector({"W", "G", "R", "Y"}, monochrome=True))

    for bucket, size in (
        ("mdpi", 48),
        ("hdpi", 72),
        ("xhdpi", 96),
        ("xxhdpi", 144),
        ("xxxhdpi", 192),
    ):
        folder = os.path.join(RES, "mipmap-" + bucket)
        write_png(os.path.join(folder, "ic_launcher.png"), size, round_mask=False)
        write_png(os.path.join(folder, "ic_launcher_round.png"), size, round_mask=True)
        print("wrote %s at %dpx" % (bucket, size))

    scale, cx, cy = safe_scale()
    print("art bounds %s, safe-circle scale %.3f, art %.1f x %.1f dp of 108" % (
        art_bounds(), scale, 12 * scale, 15 * scale))


main()
