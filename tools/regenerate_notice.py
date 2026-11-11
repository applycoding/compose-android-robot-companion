#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Regenerate third-party dependency section in NOTICE from Gradle license reports."""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORTS = [
    ROOT / "app/build/reports/dependency-license/licenses.md",
    ROOT / "sim-server/build/reports/dependency-license/licenses.md",
]
NOTICE = ROOT / "NOTICE"

DEP_LINE = re.compile(
    r"^\*\*\d+\*\* \*\*Group:\*\* `([^`]+)` \*\*Name:\*\* `([^`]+)` \*\*Version:\*\* `([^`]+)`"
)
LICENSE_LINE = re.compile(r"^> - \*\*POM License\*\*: (.+?) - ")


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
                license_name = lm.group(1).strip()
                deps[current] = license_name
    return deps


def main() -> int:
    merged: dict[str, str] = {}
    for report in REPORTS:
        if not report.is_file():
            print(f"missing report: {report}", file=sys.stderr)
            return 1
        for key, lic in parse_report(report).items():
            merged[key] = lic or merged.get(key, "")
    missing_license = [k for k, v in merged.items() if not v]
    if missing_license:
        print(f"dependencies without license metadata: {missing_license[:5]}", file=sys.stderr)
        return 1
    tools_section = """Build and test tools (not redistributed in the APK):

- Eclipse Temurin JDK 21.0.13+9 (GPL-2.0-with-classpath-exception, used to compile)
- Gradle 9.9.1 (Apache-2.0)
- Android Gradle Plugin 9.5.1 (Apache-2.0)
- Kotlin Gradle plugins 2.4.21 (Apache-2.0)
- Robolectric 4.17 (MIT)
- Roborazzi 1.78.0 (Apache-2.0)
- JUnit 4.13.2 (Eclipse Public License 1.0)
- androidx.test espresso-core 3.7.0 and androidx.test:core 1.7.0 (Apache-2.0)
- org.jetbrains.kotlin:kotlin-test 2.4.21 (Apache-2.0)
- androidx.lifecycle:lifecycle-runtime-testing 2.11.0 (Apache-2.0)
- kotlinx-coroutines-test:1.11.0 (Apache-2.0)
- androidx.compose.ui:ui-test-junit4 (Compose BOM 2026.09.00, Apache-2.0)
- com.github.jk1.dependency-license-report:3.1.5 (Apache-2.0)
- Android SDK platform android-37 and build-tools 37.0.0 (Android Software Development Kit License; used to build, not redistributed)
"""
    dep_lines = sorted(
        f"- {key} ({merged[key]})" for key in merged
    )
    body = f"""compose-android-robot-companion
Copyright 2026 Arpan Bom

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

This product bundles third-party libraries. See Gradle dependency license reports
and REFERENCES.md for provenance and public documentation links.

Third-party runtime and compile dependencies (from :app debugRuntimeClasspath and :sim-server runtimeClasspath):

{chr(10).join(dep_lines)}

{tools_section}
Refer to REFERENCES.md for external documentation and idea sources.
"""
    NOTICE.write_text(body, encoding="utf-8")
    print(f"wrote {NOTICE} ({len(dep_lines)} dependencies)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
