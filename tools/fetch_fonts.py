"""Downloads the app's two display fonts from the Google Fonts repository into res/font.

Run from anywhere:  python tools/fetch_fonts.py

- Playfair Display (variable weight): names and headings in every theme but Nothing — a classic
  high-contrast serif for the Godfather feel.
- Doto (variable weight): a dot-matrix face for the Nothing theme, in the spirit of Nothing OS.

Both are licensed under the SIL Open Font License 1.1 (see docs/THIRD_PARTY.md); the licence texts
are saved to docs/licenses/. Pure stdlib.
"""
import os
import sys
import urllib.parse
import urllib.request

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")
LICENSE_DIR = os.path.join(ROOT, "docs", "licenses")
BASE = "https://raw.githubusercontent.com/google/fonts/main/ofl"

# (resource name, family folder, file name in the repo)
FONTS = [
    ("playfair_display", "playfairdisplay", "PlayfairDisplay[wght].ttf"),
    ("doto", "doto", "Doto[ROND,wght].ttf"),
]


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


def main():
    os.makedirs(FONT_DIR, exist_ok=True)
    os.makedirs(LICENSE_DIR, exist_ok=True)
    for resource, family, file_name in FONTS:
        data = fetch(f"{BASE}/{family}/{urllib.parse.quote(file_name)}")
        if not data.startswith(b"\x00\x01\x00\x00"):
            sys.exit(f"{file_name}: not a TrueType font")
        with open(os.path.join(FONT_DIR, f"{resource}.ttf"), "wb") as out:
            out.write(data)
        licence = fetch(f"{BASE}/{family}/OFL.txt")
        with open(os.path.join(LICENSE_DIR, f"{resource}-OFL.txt"), "wb") as out:
            out.write(licence)
        print(f"{resource}.ttf  {len(data) // 1024} KB")


if __name__ == "__main__":
    main()
