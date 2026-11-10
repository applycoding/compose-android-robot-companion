#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Build GitHub Pages HTML from screenshot PNGs."""

from __future__ import annotations

import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FRAMES = [
    ("screenshot_tick_0.png", "screenshot_tick_0.png", "tick 0"),
    ("screenshot_tick_150.png", "screenshot_tick_150.png", "tick 150 RUNNING"),
    ("screenshot_tick_300.png", "screenshot_tick_300.png", "tick 300 RUNNING"),
    ("screenshot_tick_450.png", "screenshot_tick_450.png", "tick 450 RUNNING"),
    ("screenshot_tick_627.png", "screenshot_tick_627.png", "tick 627 COMPLETED"),
    ("screenshot_tick_313_estop.png", "screenshot_tick_313_estop.png", "tick 313 E-STOP latched"),
]
SCREEN_DIRS = [
    ROOT / "app/build/outputs/roborazzi",
    ROOT / "app/src/test/screenshots",
]


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as f:
        f.seek(16)
        w, h = struct.unpack(">II", f.read(8))
    return w, h


def main() -> int:
    out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "pages-site")
    out_dir.mkdir(parents=True, exist_ok=True)
    blocks: list[str] = []
    for src_name, out_name, label in FRAMES:
        src = next((d / src_name for d in SCREEN_DIRS if (d / src_name).is_file()), None)
        if src is None:
            print(f"missing frame: {src_name}", file=sys.stderr)
            return 1
        w, h = png_size(src)
        if (w, h) != (1080, 2400):
            print(f"bad size {w}x{h} for {src}", file=sys.stderr)
            return 1
        dst = out_dir / out_name
        dst.write_bytes(src.read_bytes())
        blocks.append(
            f'<figure><img src="{out_name}" width="270" height="600" alt="{label}"><figcaption>{label}</figcaption></figure>'
        )
    html = f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><title>Robot companion screenshots</title></head>
<body>
<h1>Simulation only, not a certified safety system</h1>
<p>debug build</p>
<div class="grid">{''.join(blocks)}</div>
</body></html>"""
    (out_dir / "index.html").write_text(html, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
