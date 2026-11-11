#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate NOTICE against Gradle license reports and curated tools."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NOTICE = ROOT / "NOTICE"
REPORTS = [
    ROOT / "app/build/reports/dependency-license/licenses.md",
    ROOT / "sim-server/build/reports/dependency-license/licenses.md",
]


def main() -> int:
    if not NOTICE.is_file():
        print("missing NOTICE", file=sys.stderr)
        return 1
    text = NOTICE.read_text(encoding="utf-8")
    if "REFERENCES.md" not in text:
        print("NOTICE must reference REFERENCES.md", file=sys.stderr)
        return 1
    if "Arpan Bom" not in text or "2026" not in text:
        print("NOTICE must list Arpan Bom, 2026", file=sys.stderr)
        return 1
    for report in REPORTS:
        if not report.is_file():
            print(f"missing license report: {report}", file=sys.stderr)
            return 1
    required_tools = ["JDK 21", "Gradle 9.9.1", "Kotlin 2.4.21", "Robolectric 4.17", "Roborazzi 1.78.0"]
    for item in required_tools:
        if item not in text:
            print(f"NOTICE missing tools entry: {item}", file=sys.stderr)
            return 1
    if not re.search(r"Apache-2\.0", text):
        print("NOTICE missing Apache-2.0", file=sys.stderr)
        return 1
    print("NOTICE ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
