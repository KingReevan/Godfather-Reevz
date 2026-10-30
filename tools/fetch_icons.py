"""Downloads the app's UI icons from Google's Material Symbols and writes them as vector drawables.

Run from anywhere:  python tools/fetch_icons.py

Each icon becomes app/src/main/res/drawable/ic_<key>.xml, in the Rounded style, filled (`fill1`)
where Google publishes a filled variant.

The `android:tint="?attr/colorControlNormal"` attribute is stripped: Compose's Icon applies its
own tint, and a theme-attribute reference would resolve against the Activity's XML theme instead
of the app's Compose theme.

Brand glyphs (the WhatsApp button's) come from Simple Icons, pinned to one version: a single
24x24 path, drawn a little inside its box so it sits the same size as the Material Symbols.

Material Symbols are licensed under Apache 2.0, Simple Icons under CC0 (see docs/THIRD_PARTY.md).
Same approach as Reevz Habitz's tools/fetch_habit_icons.py. Pure stdlib.
"""
import os
import re
import sys
import urllib.error
import urllib.request

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "res", "drawable")
BASE = "https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"

# (key, Material Symbols name, filled?)
ICONS = [
    ("arrow_back", "arrow_back", False),
    ("menu", "menu", False),
    ("settings", "settings", True),
    ("call", "call", True),
    ("check", "check", False),
    ("check_circle", "check_circle", True),
    ("search", "search", False),
    ("close", "close", False),
    ("contacts", "contacts", True),
    ("shuffle", "shuffle", False),
    ("undo", "undo", False),
    ("notes", "edit_note", False),
    ("history", "history", False),
    ("calendar", "calendar_month", True),
    ("streak", "local_fire_department", True),
    ("group", "group", True),
    ("hourglass", "hourglass_bottom", True),
    ("add_photo", "add_a_photo", True),
    ("delete", "delete", True),
]

SIMPLE_ICONS = "https://cdn.jsdelivr.net/npm/simple-icons@16.36.0/icons"

# (key, Simple Icons slug)
BRANDS = [
    ("whatsapp", "whatsapp"),
]

BRAND_XML = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="28"
    android:viewportHeight="28">
  <group
      android:translateX="2"
      android:translateY="2">
    <path
        android:fillColor="@android:color/white"
        android:pathData="{path}"/>
  </group>
</vector>
"""


def fetch(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def main():
    keys = [key for key, _, _ in ICONS] + [key for key, _ in BRANDS]
    if len(keys) != len(set(keys)):
        sys.exit("Duplicate keys in ICONS")

    missing = []
    for key, symbol, filled in ICONS:
        folder = f"{BASE}/{symbol}/materialsymbolsrounded"
        xml = None
        if filled:
            xml = fetch(f"{folder}/{symbol}_fill1_24px.xml")
        xml = xml or fetch(f"{folder}/{symbol}_24px.xml")
        if xml is None:
            missing.append(f"{key} ({symbol})")
            continue
        xml = re.sub(r'\s*android:tint="[^"]*"', "", xml)
        with open(os.path.join(RES, f"ic_{key}.xml"), "w", encoding="utf-8", newline="\n") as out:
            out.write(xml)

    for key, slug in BRANDS:
        svg = fetch(f"{SIMPLE_ICONS}/{slug}.svg")
        paths = re.findall(r'<path d="([^"]+)"', svg or "")
        if len(paths) != 1 or 'viewBox="0 0 24 24"' not in svg:
            missing.append(f"{key} ({slug})")
            continue
        with open(os.path.join(RES, f"ic_{key}.xml"), "w", encoding="utf-8", newline="\n") as out:
            out.write(BRAND_XML.format(path=paths[0]))

    print(f"Wrote {len(ICONS) + len(BRANDS) - len(missing)} icons to {os.path.normpath(RES)}")
    if missing:
        sys.exit("Not found: " + ", ".join(missing))


if __name__ == "__main__":
    main()
