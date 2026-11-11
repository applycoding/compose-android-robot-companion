#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Build GitHub Pages HTML from screenshot PNGs."""

from __future__ import annotations

import os
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FRAMES = [
    ("screenshot_tick_0.png", "tick 0 IDLE"),
    ("screenshot_tick_150.png", "tick 150 RUNNING"),
    ("screenshot_tick_300.png", "tick 300 RUNNING"),
    ("screenshot_tick_450.png", "tick 450 RUNNING"),
    ("screenshot_tick_627.png", "tick 627 COMPLETED"),
    ("screenshot_tick_313_estop.png", "tick 313 ABORTED E-STOP latched"),
]
SCREEN_DIRS = [
    ROOT / "app/build/outputs/roborazzi",
    ROOT / "app/src/test/screenshots",
]
LABEL = "Simulation only, not a certified safety system"
TITLE = f"{LABEL}: Robot companion"


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as f:
        f.seek(16)
        w, h = struct.unpack(">II", f.read(8))
    return w, h


def main() -> int:
    out_dir = Path(sys.argv[1] if len(sys.argv) > 1 else "pages-site")
    out_dir.mkdir(parents=True, exist_ok=True)
    run_id = os.environ.get("GITHUB_RUN_ID", "").strip()
    repo = os.environ.get("GITHUB_REPOSITORY", "").strip()
    run_link = ""
    if run_id and repo:
        run_link = (
            f'<p><a href="https://github.com/{repo}/actions/runs/{run_id}">'
            f"CI run {run_id}</a> (download app-debug-apk artifact)</p>"
        )
    blocks: list[str] = []
    for src_name, state_label in FRAMES:
        src = next((d / src_name for d in SCREEN_DIRS if (d / src_name).is_file()), None)
        if src is None:
            print(f"missing frame: {src_name}", file=sys.stderr)
            return 1
        w, h = png_size(src)
        if (w, h) != (1080, 2400):
            print(f"bad size {w}x{h} for {src}", file=sys.stderr)
            return 1
        dst = out_dir / src_name
        dst.write_bytes(src.read_bytes())
        blocks.append(
            f'<figure><img src="{src_name}" width="270" height="600" alt="{state_label}">'
            f"<figcaption>{state_label}</figcaption></figure>"
        )
    html = f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8"><title>{TITLE}</title></head>
<body>
<h1>{LABEL}</h1>
<p>debug build</p>
{run_link}
<div class="grid">{''.join(blocks)}</div>
</body></html>"""
    (out_dir / "index.html").write_text(html, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
