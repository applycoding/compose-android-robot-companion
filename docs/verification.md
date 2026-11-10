# SPDX-License-Identifier: Apache-2.0

# Verification map

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | Section 5 measured rows + golden trace | `:sim:test`, CI `jvm` |
| 2 | Mission reject precedence | `MissionRulesTest`, CI `jvm` |
| 3 | Wire codec goldens | `WireCodecTest`, CI `jvm` |
| 4 | Lockstep live trace | `LockstepLiveTest`, CI `jvm` |
| 5 | Realtime ticks + estop timing | `RealtimeLiveTest`, CI `jvm` |
| 6 | Session busy / latch | `SessionTest`, CI `jvm` |
| 7 | Controls UI estop | `ControlsUiTest`, CI `android` |
| 8 | Process death saved state | `ProcessDeathTest`, CI `android` |
| 9 | Roborazzi 1080×2400 | `ScreenshotTest`, CI `android` |
| 10 | APK badging | `tools/check_apk.sh`, CI `android` |
| 11 | Mutant kill matrix | `tools/mutants.py`, CI `mutants` |
| 12 | No dead tests | CI `hygiene` |
| 13 | NOTICE | `tools/check_notice.py`, CI `notice` |
| 14 | SPDX headers | `tools/check_spdx.py`, CI `hygiene` |
| 15 | LICENSE sha256 | CI `hygiene` |
| 16 | Pinned actions + wrapper | CI `hygiene` |
| 17 | Pages deploy | `.github/workflows/pages.yml` after merge |
| 18 | README honesty | review + grep label |
