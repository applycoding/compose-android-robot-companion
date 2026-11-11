<!-- SPDX-License-Identifier: Apache-2.0 -->

# Verification map

Each row matches criterion number in `PLAN.md` section 12.

| # | Criterion | Test or tool | CI job step |
| --- | --- | --- | --- |
| 1 | Section 5 measured rows and golden trace | `:sim:test` `KinematicsTest.reachesVMaxAtTick40`, `KinematicsTest.demoCompletesAtTick627`, `EStopTest.restsAtTick113`, `HeartbeatTest.lostAtTick209`, `GoldenTraceTest.matchesGoldenFile` | `jvm`: `./gradlew :sim:test` |
| 2 | Mission reject precedence and edge rules | `:sim:test` `MissionRulesTest.startPrecedenceEstopOverLinkLost`, `MissionRulesTest.startPrecedenceLinkLostOverBusy`, `MissionRulesTest.startAcceptsExactlySixteenWaypoints` | `jvm`: `./gradlew :sim:test` |
| 3 | Wire codec goldens and malformed input | `:link:test` `WireCodecTest.snapshotGolden`; `:sim-server:test` `RobotHostBadMessageTest.badInputReturnsBadMessageWithoutChangingState` | `jvm`: `./gradlew :link:test :sim-server:test` |
| 4 | Lockstep live trace equals in-process reference | `:sim-server:test` `LockstepLiveTest.traceEqualsInProcess` | `jvm`: `./gradlew :sim-server:test` |
| 5 | Realtime ticks and e-stop timing | `:sim-server:test` `RealtimeLiveTest.ticksAdvance` | `jvm`: `./gradlew :sim-server:test` |
| 6 | Session busy, disconnect, reconnect latch | `:sim-server:test` `SessionTest.busyReconnectAndLatch` | `jvm`: `./gradlew :sim-server:test` |
| 7 | Controls UI e-stop rendering and reject snackbar | `:app:testDebugUnitTest` `ControlsUiTest.estopLatchesAndRenders` | `android`: `./gradlew :app:testDebugUnitTest` |
| 8 | Process death saves catalog; robot state does not | `:app:testDebugUnitTest` `ProcessDeathTest.catalogSurvives` | `android`: `./gradlew :app:testDebugUnitTest` |
| 9 | Six Roborazzi frames 1080×2400, threshold 0 | `:app:verifyRoborazziDebug` `ScreenshotTest.fullAppFrames`, `ScreenshotTest.estopLatchedFrame` | `android`: `./gradlew :app:verifyRoborazziDebug` |
| 10 | Debug APK package and SDK levels | `tools/check_apk.sh` | `android`: `tools/check_apk.sh` |
| 11 | Fourteen mutants killed | `tools/mutants.py` (S1-S7, L1-L3, A1-A4) | `mutants`: `python3 tools/mutants.py` |
| 12 | No ignored, assumed, or empty tests | `hygiene`: grep on `*Test*.kt`; `tools/check_junit.py` | `hygiene`: grep step; `jvm`/`android`: `python3 tools/check_junit.py` |
| 13 | NOTICE matches license reports and tools list | `tools/check_notice.py` | `notice`: `python3 tools/check_notice.py` |
| 14 | SPDX on first-party files | `tools/check_spdx.py` | `hygiene`: `python3 tools/check_spdx.py` |
| 15 | LICENSE Apache-2.0 sha256 | `hygiene` sha256sum check | `hygiene`: `sha256sum -c` on `LICENSE` |
| 16 | Pinned actions and validated wrapper | `hygiene` unpinned-`uses` grep; `gradle/actions/wrapper-validation` | `hygiene`: wrapper validation and `uses:` grep |
| 17 | Pages deploy from `pages-site` artifact | `.github/workflows/pages.yml` | `pages` workflow after successful `push` to `main` |
| 18 | README honesty (label, limits, verification links) | Manual review; `README.md` opens with simulation label; grep `Simulation only` | `hygiene` SPDX scan includes `README.md` |

Virtual-time link transport timing is covered by `:link:test` `InProcessTransportTest.realtimeTicksOnVirtualTime` in job `jvm`.
