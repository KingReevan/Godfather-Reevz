"""Generates the Godfather Reevz launcher icon: a Don Corleone-style bust in a tuxedo, with a red
rose on the lapel, against a coral-to-sunflower sunset.

Run from anywhere:  python tools/make_icon.py [preview.png]

Writes the adaptive-icon layers to app/src/main/res/drawable:
  ic_launcher_background.xml  - the sunset gradient
  ic_launcher_foreground.xml  - the Don, in colour
  ic_launcher_monochrome.xml  - his silhouette, for Android 13+ / Nothing OS themed icons

Shapes are defined once below (absolute M/L/C/Z path commands on the 108x108 adaptive-icon canvas;
only the centre ~66x66 circle is guaranteed visible under every launcher mask) and both written as
VectorDrawable path data and, for the optional preview, rasterised with Pillow through a circular
launcher mask. The monochrome layer keeps only alpha, so its details (brows, moustache, shirt,
rose) are real holes in the silhouette rather than colours.

Edit the script, not the generated XML. Same approach as Reevz Habitz's tools/make_icon.py.
"""
import math
import os
import sys

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable")

# Sicilian Sunset, the default theme.
SKY_TOP = "#FF6F5E"   # coral
SKY_BOTTOM = "#FFC23C"  # sunflower

TUX = "#17110F"
LAPEL = "#2E2522"
SHIRT = "#F7F2EA"
SKIN = "#E7AE85"
SKIN_SHADE = "#CF9068"
HAIR = "#2A2321"
HAIR_SHINE = "#5B514C"
TEMPLE = "#9C948E"
FEATURE = "#2A1D19"
LIP = "#9A5B45"
ROSE = "#C8102E"
ROSE_DARK = "#7E0A1C"
ROSE_LIGHT = "#F0505A"
LEAF = "#3F7A35"

# Lift the figure a little so more of the tuxedo shows inside the mask.
OFFSET_Y = -2.5
# Themed-icon launchers (Nothing OS among them) draw the monochrome layer smaller than a full-bleed
# icon, so the silhouette is enlarged about this point to read at the same size.
MONO_SCALE = 1.2
MONO_PIVOT = (54, 58)


# ---- Tiny path model -------------------------------------------------------------------------

def ellipse(cx, cy, rx, ry):
    k = 0.5522847498
    return [
        ("M", cx + rx, cy),
        ("C", cx + rx, cy + ry * k, cx + rx * k, cy + ry, cx, cy + ry),
        ("C", cx - rx * k, cy + ry, cx - rx, cy + ry * k, cx - rx, cy),
        ("C", cx - rx, cy - ry * k, cx - rx * k, cy - ry, cx, cy - ry),
        ("C", cx + rx * k, cy - ry, cx + rx, cy - ry * k, cx + rx, cy),
        ("Z",),
    ]


def poly(*points):
    cmds = [("M",) + points[0]]
    cmds += [("L",) + p for p in points[1:]]
    cmds.append(("Z",))
    return cmds


def mirror(subpath):
    """Reflects a subpath across the vertical centre line (x = 54)."""
    out = []
    for cmd in subpath:
        if cmd[0] == "Z":
            out.append(cmd)
            continue
        coords = list(cmd[1:])
        for i in range(0, len(coords), 2):
            coords[i] = 108 - coords[i]
        out.append((cmd[0],) + tuple(coords))
    return out


# ---- The figure --------------------------------------------------------------------------------

# Broad and heavy-jawed: wider at the jowls than at the temples.
FACE = [
    ("M", 41.6, 46), ("C", 41.6, 38.2, 47, 33.5, 54, 33.5), ("C", 61, 33.5, 66.4, 38.2, 66.4, 46),
    ("L", 67.2, 55.5), ("C", 67.2, 63, 62, 67.4, 54, 67.4), ("C", 46, 67.4, 40.8, 63, 40.8, 55.5),
    ("Z",),
]
EAR_LEFT = ellipse(41.8, 51.5, 2.6, 4.2)
EAR_RIGHT = mirror(EAR_LEFT)
# Shadow down the right cheek and under the jaw: jowls.
JOWL = [
    ("M", 66.7, 50), ("L", 67.2, 55.5), ("C", 67.2, 63, 62, 67.4, 54, 67.4),
    ("C", 50, 67.4, 46.8, 66.2, 44.8, 64), ("C", 50, 65.4, 58.6, 64.6, 62.8, 59.6), ("C", 65.2, 57, 66.2, 53.6, 66.7, 50),
    ("Z",),
]
# Age: folds from the nose to the corners of the mouth, and bags under the eyes.
FOLD_LEFT = [("M", 51.6, 56.2), ("C", 49.4, 57.6, 48.4, 59.8, 48.8, 62.2), ("C", 48.2, 59.8, 48.8, 57.4, 51.6, 56.2), ("Z",)]
FOLD_RIGHT = mirror(FOLD_LEFT)
BAG_LEFT = [("M", 46.6, 52.2), ("C", 47.8, 53.4, 49.6, 53.4, 50.8, 52.2), ("C", 49.6, 52.8, 47.8, 52.8, 46.6, 52.2), ("Z",)]
BAG_RIGHT = mirror(BAG_LEFT)
NECK = poly((48.5, 63), (59.5, 63), (60.5, 74), (47.5, 74))

# Slicked back, receding at the temples, a little peak in the middle.
HAIR_SHAPE = [
    ("M", 40.6, 49), ("C", 39.2, 36.5, 45.2, 29.5, 54, 29.5), ("C", 62.8, 29.5, 68.8, 36.5, 67.4, 49),
    ("C", 66.6, 44, 65.4, 41.2, 62.6, 40.2), ("C", 59.6, 39.2, 57, 40.4, 54, 42.4),
    ("C", 51, 40.4, 48.4, 39.2, 45.4, 40.2), ("C", 42.6, 41.2, 41.4, 44, 40.6, 49),
    ("Z",),
]
# Grey at the temples.
TEMPLE_LEFT = [("M", 40.7, 48.6), ("C", 40.6, 46, 41.4, 43.6, 43.4, 42.2), ("C", 42.6, 44.2, 42.2, 46.4, 41.9, 49.6), ("Z",)]
TEMPLE_RIGHT = mirror(TEMPLE_LEFT)
# The hairline, for the monochrome layer: a thin band just under the hair's lower edge, cut out
# of the face so the hair reads as hair rather than a bald head.
HAIRLINE = [
    ("M", 67.4, 49), ("C", 66.6, 44, 65.4, 41.2, 62.6, 40.2), ("C", 59.6, 39.2, 57, 40.4, 54, 42.4),
    ("C", 51, 40.4, 48.4, 39.2, 45.4, 40.2), ("C", 42.6, 41.2, 41.4, 44, 40.6, 49),
    ("L", 41.4, 50.6), ("C", 42.2, 45.8, 43.4, 43, 45.8, 42.0), ("C", 48.4, 41.1, 51, 42.2, 54, 44.2),
    ("C", 57, 42.2, 59.6, 41.1, 62.2, 42.0), ("C", 64.6, 43, 65.8, 45.8, 66.6, 50.6),
    ("Z",),
]
# Comb lines: thin crescents swept back over the crown.
SHINE = [
    [("M", 47, 33.2), ("C", 50.5, 31.4, 57.5, 31.4, 61, 33.2), ("C", 57.5, 32.4, 50.5, 32.4, 47, 33.2), ("Z",)],
    [("M", 44.2, 37.2), ("C", 48.5, 34.4, 59.5, 34.4, 63.8, 37.2), ("C", 59.5, 35.5, 48.5, 35.5, 44.2, 37.2), ("Z",)],
]

# Heavy brows, angled down towards the nose: stern.
BROW_LEFT = poly((44.6, 45.6), (51.6, 46.9), (51.4, 48.7), (44.8, 47.5))
BROW_RIGHT = mirror(BROW_LEFT)
# Hooded, heavy-lidded eyes: only the lower half shows.
EYE_LEFT = [("M", 46.6, 50.3), ("L", 50.3, 50.3), ("C", 49.5, 51.8, 47.4, 51.8, 46.6, 50.3), ("Z",)]
EYE_RIGHT = mirror(EYE_LEFT)
NOSE = [("M", 54.3, 49.5), ("L", 55.9, 56.6), ("C", 54.8, 57.2, 53.2, 57.2, 52.2, 56.6), ("C", 53.2, 56.4, 54.2, 56.3, 54.9, 56), ("Z",)]
MOUSTACHE = [
    ("M", 49.2, 59.4), ("C", 51.6, 57.9, 56.4, 57.9, 58.8, 59.4), ("L", 58.6, 60.3),
    ("C", 56.2, 59.3, 51.8, 59.3, 49.4, 60.3), ("Z",),
]
MOUTH = [("M", 50.6, 61.6), ("C", 52.8, 61.1, 55.2, 61.1, 57.4, 61.6), ("L", 57.3, 62.3), ("C", 55.2, 61.9, 52.8, 61.9, 50.7, 62.3), ("Z",)]

# The tuxedo in two halves either side of the shirt, so the shirt is a real gap (no hole needed).
JACKET_LEFT = [
    ("M", 14, 110), ("L", 14, 99), ("C", 14, 87, 22.5, 79.5, 35, 76.2), ("L", 47, 72.5),
    ("L", 54, 102), ("L", 54, 110), ("Z",),
]
JACKET_RIGHT = mirror(JACKET_LEFT)
LAPEL_LEFT = poly((47, 72.5), (54, 102), (50.2, 103), (39.6, 81), (37.2, 76.6))
LAPEL_RIGHT = mirror(LAPEL_LEFT)
SHIRT_V = poly((46.6, 72.5), (61.4, 72.5), (54, 102))
COLLAR_LEFT = poly((47.6, 70.5), (53, 74.5), (49, 76.5))
COLLAR_RIGHT = mirror(COLLAR_LEFT)
BOW = [
    ("M", 54, 76.2), ("L", 46.2, 72.2), ("C", 45.2, 74.6, 45.2, 77.8, 46.2, 80.2), ("Z",),
    ("M", 54, 76.2), ("L", 61.8, 72.2), ("C", 62.8, 74.6, 62.8, 77.8, 61.8, 80.2), ("Z",),
] + ellipse(54, 76.2, 1.9, 2.1)

# A red rose on his left lapel (the viewer's right), as in the wedding scene.
ROSE_CENTRE = (63.6, 81.0)
LEAF_SHAPE = [("M", 60.2, 85.6), ("C", 60.6, 82.8, 62.8, 82.2, 63.8, 83.4), ("C", 63.2, 85.4, 61.6, 86.2, 60.2, 85.6), ("Z",)]
ROSE_OUTER = ellipse(ROSE_CENTRE[0], ROSE_CENTRE[1], 3.9, 3.6)
ROSE_INNER = ellipse(ROSE_CENTRE[0] + 0.2, ROSE_CENTRE[1] - 0.4, 2.2, 2.0)
ROSE_SWIRL = [
    ("M", 62.3, 80.9), ("C", 62.6, 79.4, 64.8, 79.3, 65.0, 80.8), ("C", 64.1, 80.0, 63.0, 80.1, 62.3, 80.9), ("Z",),
]

# (colour, fill type, subpaths) — painted in order.
FOREGROUND = [
    (TUX, "nonZero", [JACKET_LEFT, JACKET_RIGHT]),
    (SHIRT, "nonZero", [SHIRT_V]),
    (LAPEL, "nonZero", [LAPEL_LEFT, LAPEL_RIGHT]),
    (SKIN_SHADE, "nonZero", [NECK]),
    (SHIRT, "nonZero", [COLLAR_LEFT, COLLAR_RIGHT]),
    (TUX, "nonZero", [BOW]),
    (SKIN_SHADE, "nonZero", [EAR_LEFT, EAR_RIGHT]),
    (SKIN, "nonZero", [FACE]),
    (SKIN_SHADE, "nonZero", [JOWL, NOSE, FOLD_LEFT, FOLD_RIGHT, BAG_LEFT, BAG_RIGHT]),
    (HAIR, "nonZero", [HAIR_SHAPE]),
    (HAIR_SHINE, "nonZero", SHINE),
    (TEMPLE, "nonZero", [TEMPLE_LEFT, TEMPLE_RIGHT]),
    (FEATURE, "nonZero", [BROW_LEFT, BROW_RIGHT, EYE_LEFT, EYE_RIGHT, MOUSTACHE]),
    (LIP, "nonZero", [MOUTH]),
    (LEAF, "nonZero", [LEAF_SHAPE]),
    (ROSE, "nonZero", [ROSE_OUTER]),
    (ROSE_LIGHT, "nonZero", [ROSE_INNER]),
    (ROSE_DARK, "nonZero", [ROSE_SWIRL]),
]

# Silhouette: jacket halves (the shirt is the gap between them) with the rose cut out of the
# right one, the bow tie inside the gap, and the head with hairline, comb lines, brows, eyes and
# moustache cut out.
MONOCHROME = [
    ("#FFFFFF", "evenOdd", [JACKET_LEFT, JACKET_RIGHT + ROSE_OUTER]),
    ("#FFFFFF", "nonZero", [BOW]),
    ("#FFFFFF", "nonZero", [NECK[:-1] + [("Z",)]]),
    ("#FFFFFF", "evenOdd", [FACE + HAIRLINE + BROW_LEFT + BROW_RIGHT + EYE_LEFT + EYE_RIGHT + MOUSTACHE]),
    ("#FFFFFF", "nonZero", [EAR_LEFT, EAR_RIGHT]),
    # The comb lines cut into the hair, so it reads as slicked-back hair, not a bald head.
    ("#FFFFFF", "evenOdd", [HAIR_SHAPE + SHINE[0] + SHINE[1]]),
]


# ---- Writing VectorDrawables -------------------------------------------------------------------

def fmt(value):
    text = f"{value:.2f}".rstrip("0").rstrip(".")
    return "0" if text == "-0" else text


def path_data(subpaths):
    parts = []
    for subpath in subpaths:
        for cmd in subpath:
            if cmd[0] == "Z":
                parts.append("Z")
            else:
                parts.append(cmd[0] + ",".join(fmt(v) for v in cmd[1:]))
    return "".join(parts)


def vector(body, extra_ns=""):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/make_icon.py. Edit the script, not this file. -->\n"
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{extra_ns}\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
        f"{body}"
        "</vector>\n"
    )


def layer(shapes, scale=1.0):
    # Scale about the pivot (in canvas coordinates, after the lift), then lift.
    zoom = (
        f' android:scaleX="{fmt(scale)}" android:scaleY="{fmt(scale)}"'
        f' android:pivotX="{fmt(MONO_PIVOT[0])}" android:pivotY="{fmt(MONO_PIVOT[1] + OFFSET_Y)}"'
    )
    lines = [f"    <group{zoom if scale != 1.0 else ''}>\n"]
    lines.append(f'        <group android:translateY="{fmt(OFFSET_Y)}">\n')
    for colour, fill_type, subpaths in shapes:
        lines.append(
            "            <path\n"
            f'                android:fillColor="{colour}"\n'
            f'                android:fillType="{fill_type}"\n'
            f'                android:pathData="{path_data(subpaths)}" />\n'
        )
    lines.append("        </group>\n")
    lines.append("    </group>\n")
    return "".join(lines)


def background():
    body = (
        '    <path android:pathData="M0,0H108V108H0Z">\n'
        '        <aapt:attr name="android:fillColor">\n'
        '            <gradient\n'
        '                android:type="linear"\n'
        '                android:startX="54" android:startY="0"\n'
        '                android:endX="54" android:endY="108">\n'
        f'                <item android:offset="0" android:color="{SKY_TOP}" />\n'
        f'                <item android:offset="1" android:color="{SKY_BOTTOM}" />\n'
        "            </gradient>\n"
        "        </aapt:attr>\n"
        "    </path>\n"
    )
    return vector(body, extra_ns='\n    xmlns:aapt="http://schemas.android.com/aapt"')


def write(name, text):
    with open(os.path.join(RES, name), "w", encoding="utf-8", newline="\n") as out:
        out.write(text)


# ---- Preview -----------------------------------------------------------------------------------

def flatten(subpath, scale, dy, zoom=1.0):
    points, x, y = [], 0.0, 0.0
    for cmd in subpath:
        if cmd[0] in ("M", "L"):
            x, y = cmd[1], cmd[2]
            points.append((x, y))
        elif cmd[0] == "C":
            x1, y1, x2, y2, x3, y3 = cmd[1:]
            for i in range(1, 17):
                t = i / 16
                mt = 1 - t
                points.append((
                    mt ** 3 * x + 3 * mt * mt * t * x1 + 3 * mt * t * t * x2 + t ** 3 * x3,
                    mt ** 3 * y + 3 * mt * mt * t * y1 + 3 * mt * t * t * y2 + t ** 3 * y3,
                ))
            x, y = x3, y3
    cx, cy = MONO_PIVOT[0], MONO_PIVOT[1] + dy
    return [((cx + (px - cx) * zoom) * scale, (cy + (py + dy - cy) * zoom) * scale) for px, py in points]


def split_subpaths(subpaths):
    """A subpath list may hold several M...Z runs in one list; split them apart."""
    runs = []
    for subpath in subpaths:
        current = []
        for cmd in subpath:
            if cmd[0] == "M" and current:
                runs.append(current)
                current = []
            current.append(cmd)
        if current:
            runs.append(current)
    return runs


def render(shapes, size, background_fn, zoom=1.0):
    from PIL import Image, ImageChops, ImageDraw

    scale = size / 108
    image = background_fn(size)
    for colour, fill_type, subpaths in shapes:
        mask = Image.new("L", (size, size), 0)
        for run in split_subpaths(subpaths):
            piece = Image.new("L", (size, size), 0)
            ImageDraw.Draw(piece).polygon(flatten(run, scale, OFFSET_Y, zoom), fill=255)
            mask = ImageChops.logical_xor(mask.convert("1"), piece.convert("1")).convert("L") \
                if fill_type == "evenOdd" else ImageChops.lighter(mask, piece)
        image.paste(Image.new("RGB", (size, size), colour), (0, 0), mask)
    return image


def sunset(size):
    from PIL import Image

    top = tuple(int(SKY_TOP[i:i + 2], 16) for i in (1, 3, 5))
    bottom = tuple(int(SKY_BOTTOM[i:i + 2], 16) for i in (1, 3, 5))
    image = Image.new("RGB", (size, size))
    for row in range(size):
        t = row / (size - 1)
        image.paste(tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)), (0, row, size, row + 1))
    return image


def preview(path):
    from PIL import Image, ImageDraw

    size = 864  # 8x, downsampled for smooth edges
    colour = render(FOREGROUND, size, sunset)
    mono = render(MONOCHROME, size, lambda s: Image.new("RGB", (s, s), "#3D3A38"), MONO_SCALE)

    def masked(image):
        mask = Image.new("L", (size, size), 0)
        radius = 33 / 108 * size
        centre = size / 2
        ImageDraw.Draw(mask).ellipse((centre - radius, centre - radius, centre + radius, centre + radius), fill=255)
        out = Image.new("RGB", (size, size), "white")
        out.paste(image, (0, 0), mask)
        return out.crop((int(centre - radius), int(centre - radius), int(centre + radius), int(centre + radius)))

    tiles = [masked(colour), masked(mono), colour]
    tile = 288
    sheet = Image.new("RGB", (tile * 3 + 40, tile + 20), "white")
    for i, image in enumerate(tiles):
        sheet.paste(image.resize((tile, tile), Image.LANCZOS), (10 + i * (tile + 10), 10))
    sheet.save(path)
    print(f"Preview: {path}")


def main():
    write("ic_launcher_background.xml", background())
    write("ic_launcher_foreground.xml", vector(layer(FOREGROUND)))
    write("ic_launcher_monochrome.xml", vector(layer(MONOCHROME, MONO_SCALE)))
    print(f"Wrote launcher layers to {os.path.normpath(RES)}")
    if len(sys.argv) > 1:
        preview(sys.argv[1])


if __name__ == "__main__":
    main()
