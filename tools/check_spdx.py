#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_PREFIXES = (".git/", "build/", ".gradle/", "gradle/wrapper/")
FIRST_PARTY_SUFFIX = {".kt", ".kts", ".toml", ".properties", ".yml", ".py", ".sh", ".xml", ".md"}


def iter_files() -> list[Path]:
    out: list[Path] = []
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        rel = path.relative_to(ROOT).as_posix()
        if "/build/" in rel or rel.startswith(SKIP_PREFIXES) or any(rel.startswith(p) for p in SKIP_PREFIXES):
            continue
        if path.name == "local.properties":
            continue
        if path.suffix in FIRST_PARTY_SUFFIX:
            out.append(path)
        if "screenshots" in rel and path.suffix == ".png":
            out.append(path)
    return out


def header_ok(path: Path) -> bool:
    if path.suffix == ".png":
        lic = path.with_suffix(path.suffix + ".license")
        return lic.is_file() and "Apache-2.0" in lic.read_text(encoding="utf-8")
    if path.name == "LICENSE":
        return True
    text = path.read_text(encoding="utf-8")
    if path.suffix == ".xml":
        return bool(re.search(r"SPDX-License-Identifier:\s*Apache-2\.0", text))
    if path.suffix == ".md":
        return bool(re.search(r"SPDX-License-Identifier:\s*Apache-2\.0", text[:300]))
    first = "\n".join(text.splitlines()[:3])
    return "SPDX-License-Identifier: Apache-2.0" in first


def main() -> int:
    files = iter_files()
    missing = [p for p in files if not header_ok(p)]
    if missing:
        for p in missing:
            print(f"missing SPDX: {p.relative_to(ROOT)}", file=sys.stderr)
        return 1
    print(f"SPDX ok on {len(files)} files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
