# SPDX-License-Identifier: Apache-2.0

# Tasks: compose-android-robot-companion

Do the tasks in this order. A task is done when its acceptance check passes both locally and in CI. Criterion numbers refer to `PLAN.md` section 12.

Never weaken a test, tick, threshold or bound to get a green run. Fix the code instead, or record a measured reason in `DEVIATIONS.md`.

## T1. Build skeleton and hygiene

- Gradle wrapper 9.9.1 (with its sha256), `settings.gradle.kts`, `gradle/libs.versions.toml` with the pins from PLAN section 3, and the four modules.
- `ci.yml` job `hygiene`, plus `tools/check_spdx.py`.
- Accept: criteria 14, 15 and 16.

## T2. Simulator

- `:sim` exactly as in PLAN section 5.
- Tests for every measured row and every rule.
- `golden/demo_trace.txt`, written by a small `main` in the test sources and compared line by line.
- Accept: criteria 1 and 2.

## T3. Link

- Wire types, a strict codec with one golden JSON per message type, `Transport`, `InProcessTransport` with an injectable tick source, `WebSocketTransport` (Ktor CIO), and `HeartbeatSender`.
- Accept: criterion 3, plus the virtual-time tests.

## T4. Server

- `RobotHost` (one long-lived robot, one owner coroutine, bounded outbound buffer), the Ktor server bound to loopback, and the realtime and lockstep modes.
- `LockstepLiveTest`, `RealtimeLiveTest` and `SessionTest`.
- Accept: criteria 4, 5 and 6.

## T5. App

- `MainActivity` (with an SPDX header), `RobotViewModel`, `UiState`/`UiEvent`, the UI components, the debug network security config, and the settings toggle.
- `ControlsUiTest` and `ProcessDeathTest` under Robolectric SDK 37, with the JVM arguments and test pins from PLAN section 3.
- Accept: criteria 7 and 8.

## T6. Screenshots and APK

- `ScreenshotTest` with the six frames at threshold 0, and goldens recorded on the CI environment.
- `tools/check_apk.sh` and `tools/build_pages.py`.
- `ci.yml` job `android`, uploading the `app-debug-apk` and `pages-site` artifacts.
- Accept: criteria 9 and 10.

## T7. Mutants

- `tools/mutants.py` with all 13 mutants from PLAN section 9, run in a temporary copy, with JUnit XML classification.
- `ci.yml` job `mutants`.
- Accept: criterion 11. The kill matrix is in the job log.

## T8. NOTICE, README, docs

- `dependency-license-report` 3.1.5, the curated tools section, `tools/check_notice.py`, and the `ci.yml` job `notice`.
- `README.md`, with the content listed in criterion 18 and PLAN section 14.
- `docs/verification.md`, mapping each criterion to its test and CI job.
- Accept: criteria 12, 13 and 18.

## T9. Pages

- `pages.yml` as in PLAN section 11.
- Accept: criterion 17, once the change is merged to `main`.
