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

DEP_LINE = re.compile(
    r"^\*\*\d+\*\* \*\*Group:\*\* `([^`]+)` \*\*Name:\*\* `([^`]+)` \*\*Version:\*\* `([^`]+)`"
)
LICENSE_LINE = re.compile(r"^> - \*\*POM License\*\*: (.+?) - ")
NOTICE_DEP = re.compile(r"^- ([^ ]+) \((.+)\)\s*$")

CURATED_TOOLS = [
    ("Eclipse Temurin JDK 21.0.13+9", "GPL-2.0-with-classpath-exception"),
    ("Gradle 9.9.1", "Apache-2.0"),
    ("Android Gradle Plugin 9.5.1", "Apache-2.0"),
    ("Kotlin Gradle plugins 2.4.21", "Apache-2.0"),
    ("Robolectric 4.17", "MIT"),
    ("Roborazzi 1.78.0", "Apache-2.0"),
    ("JUnit 4.13.2", "Eclipse Public License 1.0"),
    ("androidx.test espresso-core 3.7.0", "Apache-2.0"),
    ("androidx.test:core 1.7.0", "Apache-2.0"),
    ("org.jetbrains.kotlin:kotlin-test 2.4.21", "Apache-2.0"),
    ("androidx.lifecycle:lifecycle-runtime-testing 2.11.0", "Apache-2.0"),
    ("Android SDK platform android-37", "Android Software Development Kit License"),
]


def parse_report(path: Path) -> dict[str, str]:
    deps: dict[str, str] = {}
    current: str | None = None
    for line in path.read_text(encoding="utf-8").splitlines():
        m = DEP_LINE.match(line.strip())
        if m:
            g, a, v = m.groups()
            current = f"{g}:{a}:{v}"
            deps.setdefault(current, "")
            continue
        if current:
            lm = LICENSE_LINE.match(line.strip())
            if lm and not deps[current]:
                deps[current] = lm.group(1).strip()
    missing = [k for k, v in deps.items() if not v]
    if missing:
        raise SystemExit(f"report {path} missing license for {missing[0]}")
    return deps


def parse_notice_deps(text: str) -> dict[str, str]:
    in_deps = False
    deps: dict[str, str] = {}
    for line in text.splitlines():
        if line.startswith("Third-party runtime"):
            in_deps = True
            continue
        if in_deps and line.startswith("Build and test tools"):
            break
        if in_deps:
            m = NOTICE_DEP.match(line)
            if m:
                deps[m.group(1)] = m.group(2)
    return deps


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
    report_deps: dict[str, str] = {}
    for report in REPORTS:
        if not report.is_file():
            print(f"missing license report: {report}", file=sys.stderr)
            return 1
        report_deps.update(parse_report(report))
    notice_deps = parse_notice_deps(text)
    report_keys = set(report_deps)
    notice_keys = set(notice_deps)
    missing = report_keys - notice_keys
    extra = notice_keys - report_keys
    if missing:
        print(f"NOTICE missing dependencies: {sorted(missing)[:5]}...", file=sys.stderr)
        return 1
    if extra:
        print(f"NOTICE extra dependencies not in reports: {sorted(extra)[:5]}...", file=sys.stderr)
        return 1
    for key in report_keys:
        if notice_deps[key] != report_deps[key]:
            print(f"license mismatch for {key}: notice={notice_deps[key]} report={report_deps[key]}", file=sys.stderr)
            return 1
    for tool, lic in CURATED_TOOLS:
        if tool not in text or lic not in text:
            print(f"NOTICE missing curated tool entry: {tool} ({lic})", file=sys.stderr)
            return 1
    print("NOTICE ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
