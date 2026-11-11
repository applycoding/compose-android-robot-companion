#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESULT_DIRS = [
    ROOT / "sim/build/test-results/test",
    ROOT / "link/build/test-results/test",
    ROOT / "sim-server/build/test-results/test",
    ROOT / "app/build/test-results/testDebugUnitTest",
]


def main() -> int:
    skipped: list[str] = []
    for result_dir in RESULT_DIRS:
        if not result_dir.is_dir():
            print(f"missing test results: {result_dir}", file=sys.stderr)
            return 1
        for xml in result_dir.glob("TEST-*.xml"):
            tree = ET.parse(xml)
            root = tree.getroot()
            for case in root.findall("testcase"):
                if case.find("skipped") is not None:
                    skipped.append(f"{xml.name}:{case.get('name')}")
    if skipped:
        for line in skipped:
            print(f"skipped test: {line}", file=sys.stderr)
        return 1
    print("junit ok: 0 skipped")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
