# compose-android-robot-companion: plan

A native Android companion app, written in Kotlin and Jetpack Compose, for **one simulated robot**. It shows live telemetry, keeps a mission list (create, select, start, pause, resume, cancel), and has a latched e-stop. The simulator in the robot enforces every safety rule. The app sends commands and shows state, and it cannot bypass the simulator's rules.

**Simulation only, not a certified safety system.** The APK is a **debug build**. No iOS, no Play Store.

Original implementation, inspired by the public ideas in `REFERENCES.md`. License: Apache-2.0. Author: Arpan Bom.

## 1. What makes this different

- **Native Android, written from scratch.** Kotlin, Compose, coroutines and `StateFlow`, unidirectional data flow, `collectAsStateWithLifecycle`, and `SavedStateHandle` for restoring state after process death.
- **Not a port of flutter-robot-operator.** No code, constants, protocol or layout is shared with [applycoding/flutter-robot-operator](https://github.com/applycoding/flutter-robot-operator). The simulator is a separate, pure-Kotlin module with its own model:
  - 25 Hz tick;
  - unicycle kinematics with a heading-rate limit;
  - anticipatory braking on the final approach;
  - e-stop **aborts** the mission;
  - heartbeat loss gives a protective **pause** that the operator can resume.

  The WebSocket protocol is new and owes that design nothing beyond the general idea.
- **No emulator in CI.** All UI tests run on the JVM with Robolectric, the Compose UI test library and Roborazzi screenshots. Section 13 explains why this is more reliable than an emulator job.

## 2. Scope

In scope:

- **Gradle modules:**
  - `:sim`: pure Kotlin/JVM, deterministic;
  - `:link`: wire codec, transport interface, in-process and WebSocket transports;
  - `:sim-server`: a JVM server running one long-lived robot behind a WebSocket;
  - `:app`: Compose app.
- **Tests:**
  - JVM UI tests and screenshot tests;
  - a live-stack test of the real server and the real client transport over localhost;
  - source mutants.
- **CI artifacts:** the debug APK.
- **GitHub Pages:** a page of screenshots from the tested run.

Out of scope:

- real robots, ROS and maps;
- multiple robots, authentication and TLS;
- release signing, R8 and a release build;
- iOS, the Play Store and accessibility certification;
- emulator or device tests in CI;
- any certified safety claim.

## 3. Pinned stack (checked against the registries on 2026-11-10)

| Item | Pin | Notes |
| --- | --- | --- |
| JDK | Temurin 21.0.13+9 (`actions/setup-java`, `distribution: temurin`, `java-version: 21.0.13+9`) | Robolectric SDK 37 needs Java 21 |
| Gradle | 9.9.1 wrapper, `distributionSha256Sum=d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228` | AGP 9.5 needs at least 9.6.0 |
| AGP | 9.5.1 | built-in Kotlin, so no `kotlin-android` plugin |
| Kotlin | 2.4.21: `org.jetbrains.kotlin.jvm`, `plugin.compose`, `plugin.serialization` | `buildEnvironment` shows AGP's `kotlin-gradle-plugin:2.2.10 -> 2.4.21` (measured) |
| Compose BOM | 2026.09.00 | resolves `ui` 1.12.1, `ui-test-junit4` 1.12.1, `material3` 1.4.0 (measured) |
| activity-compose | 1.13.0 | |
| lifecycle (runtime-compose, viewmodel-compose, viewmodel-savedstate) | 2.11.0 | |
| kotlinx-coroutines (core, android, test) | 1.11.0 | |
| kotlinx-serialization-json | 1.12.0 | |
| Ktor (server-core, server-cio, server-websockets, client-core, client-cio, client-websockets) | 3.6.1 | CIO supports WebSockets on Android; the Ktor `Android` engine does not |
| Robolectric | 4.17, `@Config(sdk = [37])` | |
| androidx.test espresso-core / core | 3.7.0 / 1.7.0, pinned explicitly | the BOM's transitive espresso 3.5.1 fails on SDK 37 with `NoSuchMethodException: InputManager.getInstance()` (measured) |
| Roborazzi (plugin, core, compose, junit-rule) | 1.78.0 | |
| JUnit | 4.13.2 | |
| dependency-license-report | `com.github.jk1.dependency-license-report` 3.1.5 | measured working on AGP 9.5.1 `debugRuntimeClasspath` |
| Android SDK | `platforms;android-37.0`, `build-tools;37.0.0`, `platform-tools` | compileSdk 37, targetSdk 37, minSdk 26 |

GitHub Actions, pinned by full commit SHA:

```
actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1              # v7.0.1
actions/setup-java@77dd5b2ce225ea11469928ac317850245a74ae78            # v6.2.0
gradle/actions/setup-gradle@8da46b539cd8fe6ab27ca75128919a18d757604c   # v6.4.1
gradle/actions/wrapper-validation@8da46b539cd8fe6ab27ca75128919a18d757604c # v6.4.1
actions/upload-artifact@cf430e030ddbb5b0abf93d22962f4752f3646cd9       # v7.0.2
actions/download-artifact@9000827ccba6bdab643e8b6fd33ac0654aef8333     # v8.0.2
actions/configure-pages@45bfe0192ca1faeb007ade9deae92b16b8254a0d       # v6.0.0
actions/upload-pages-artifact@fc324d3547104276b827a68afc52ff2a11cc49c9 # v5.0.0
actions/deploy-pages@368f82528645a54fb793d4d04e342629a3f51346          # v5.0.1
```

All versions live in `gradle/libs.versions.toml`, and every module uses `jvmToolchain(21)`.

The Robolectric test JVM needs this argument (measured):

```
--add-exports=java.base/jdk.internal.access=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED
```

It goes in `testOptions.unitTests.all { jvmArgs(...) }`. Without it, Robolectric fails with `Failed to interact with raw FileDescriptor internals`.

## 4. Layout

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/*, gradlew, gradlew.bat
sim/src/main/kotlin/com/applycoding/robotcompanion/sim/{SimConfig,Sim,Types,DemoMissions}.kt
sim/src/test/kotlin/.../sim/{KinematicsTest,EStopTest,HeartbeatTest,MissionRulesTest,BatteryTest,GoldenTraceTest}.kt
sim/src/test/resources/golden/demo_trace.txt
link/src/main/kotlin/com/applycoding/robotcompanion/link/{Wire,WireCodec,Transport,InProcessTransport,WebSocketTransport,HeartbeatSender}.kt
link/src/test/kotlin/.../link/{WireCodecTest,InProcessTransportTest,HeartbeatSenderTest}.kt
link/src/test/resources/wire/*.json            # one golden frame per message type
sim-server/src/main/kotlin/com/applycoding/robotcompanion/server/{RobotHost,Server,Main}.kt
sim-server/src/test/kotlin/.../server/{LockstepLiveTest,RealtimeLiveTest,SessionTest}.kt
app/src/main/AndroidManifest.xml, app/src/debug/res/xml/network_security_config.xml
app/src/main/kotlin/com/applycoding/robot_companion/{MainActivity,RobotViewModel,UiState,CompanionApp}.kt
app/src/main/kotlin/com/applycoding/robot_companion/ui/{TelemetryBar,MapPanel,MissionList,MissionEditor,ControlRow,EStopButton,Banner}.kt
app/src/main/res/values/strings.xml, app/src/main/res/drawable/ic_launcher_foreground.xml (authored vector)
app/src/test/kotlin/com/applycoding/robot_companion/{ViewModelTest,ProcessDeathTest,ControlsUiTest,ScreenshotTest}.kt
app/src/test/screenshots/*.png                 # Roborazzi goldens
tools/mutants.py, tools/check_spdx.py, tools/check_notice.py, tools/build_pages.py, tools/check_apk.sh
.github/workflows/ci.yml, .github/workflows/pages.yml
NOTICE, LICENSE, README.md, REFERENCES.md, PLAN.md, TASKS.md, docs/verification.md
```

## 5. Simulator (`:sim`, settled)

This module is pure Kotlin. It has no Android imports, no coroutines, no clock, no `Random` and no I/O. Time advances only when `step()` is called.

### Constants (`SimConfig.standard`)

| Constant | Value |
| --- | --- |
| `dtMs` | 40 (25 Hz) |
| `vMax` | 0.8 m/s |
| `aMax` | 0.5 m/s² (speed up, slow down, operator pause) |
| `brake` | 1.6 m/s² (e-stop, link-lost pause) |
| `wMax` | 1.2 rad/s |
| `kHeading` | 2.0 |
| `arriveTol` | 0.10 m |
| `heartbeatTimeoutTicks` | 13 |
| room | 10 × 8 m, waypoints must be in [0.5, 9.5] × [0.5, 7.5] |
| start pose | (1, 1), θ = 0 |
| battery | starts at 100 %; drains `0.001` per tick plus `0.01 × v/vMax` per tick; start is refused below 20 %; the mission pauses (`LOW_BATTERY`) below 10 % |
| `maxWaypoints` | 16 per mission (mission names are 1 to 32 characters) |

The demo mission is `(8,1) → (8,6) → (2,6)`.

### Tick order (exact)

`apply(cmd)` may be called any number of times between steps. Commands take effect immediately, in the order they are applied. `Heartbeat` sets `lastHb = tick`.

`step()` does the following, in this order:

1. `tick += 1`.
2. `linkOk = tick − lastHb ≤ 13`.
3. If the mission is `RUNNING` and the link is down, set `PAUSED(LINK_LOST)`. Else, if the mission is `RUNNING` and the battery is below 10 %, set `PAUSED(LOW_BATTERY)`.
4. **Motion when latched or not running.**
   - `w = 0`.
   - `v = max(0, v − d·dt)`, where `d = brake` if latched or `LINK_LOST`, else `d = aMax`.
5. **Motion when running.** Let `target` be the current waypoint and `dist` the distance to it.
   - **Waypoint switch.** If `dist ≤ arriveTol` and this is not the last waypoint, move to the next waypoint.
   - **Completion.** If `dist ≤ arriveTol` on the last waypoint and `v ≤ aMax·dt`, the mission is `COMPLETED` and `v = w = 0`.
   - **Steering.** Otherwise:
     - `e = wrap(atan2 − θ)`;
     - `w = clamp(kHeading·e, ±wMax)`;
     - `vt = vMax·max(0, cos e)`;
     - on the last waypoint, `vt = min(vt, sqrt(2·aMax·max(0, dist − arriveTol/2)))`. This is the anticipatory braking.
   - **Speed.** `v += clamp(vt − v, ±aMax·dt)`.
6. **Integrate.**
   - `θ = wrap(θ + w·dt)`.
   - `x` and `y` advance by `v·cos θ·dt` and `v·sin θ·dt`, clamped to the room.
   - `travelled += step length`.
7. **Battery.** Drain as in the constants table.
8. Return `Telemetry`:
   - `tick`, `x`, `y`, `theta`, `v`, `w`, `batteryPct`;
   - `missionId`, `mission`, `pause`, `waypointIndex`;
   - `estopLatched`, `linkOk`, `travelled`.

### Command rules (robot side, results are `Reject` codes)

- `StartMission(id, waypoints)`. Checked in this order:
  - `ESTOP_LATCHED`;
  - `LINK_LOST`;
  - `BUSY` (the mission is `RUNNING` or `PAUSED`);
  - `INVALID_MISSION` (0 or more than 16 waypoints, any coordinate non-finite, or outside the room margin);
  - `LOW_BATTERY`.

  On success: index 0, `RUNNING`.
- `Pause`. `NOT_RUNNING` unless the mission is `RUNNING`. On success: `PAUSED(OPERATOR)`.
- `Resume`. Checked in this order: `ESTOP_LATCHED`, `NOT_PAUSED`, `LINK_LOST`, `LOW_BATTERY` (if the pause reason is low battery). On success: `RUNNING`.
- `Cancel`. `NOT_RUNNING` unless the mission is `RUNNING` or `PAUSED`. On success: `CANCELLED`.
- `EStop`. Always accepted and idempotent. Latches, and turns a `RUNNING` or `PAUSED` mission into `ABORTED`. While latched, everything except `Heartbeat`, `EStop` and `ResetEStop` is rejected with `ESTOP_LATCHED`.
- `ResetEStop`. Checked in this order: `NOT_LATCHED`, `MOVING` (`v ≠ 0`), `LINK_LOST`. On success: the latch clears, the mission stays `ABORTED`, and nothing restarts automatically.

### Measured values (prototype on JDK 21.0.13; each one becomes a fixed-tick assertion)

| Scenario: heartbeat applied whenever `tick % 5 == 0` before the step | Value |
| --- | --- |
| first tick at `v = vMax` | 40 |
| waypoint switches | to index 1 at tick 237, to index 2 at tick 407 |
| demo `COMPLETED` | tick 627 (`RUNNING` at tick 626). Final pose (2.0339, 6.0031), 0.034 m from the goal. Travelled 18.399 m, battery 93.623 % |
| pose at tick 300 | (8.44, 2.54), v = 0.80, `RUNNING` (the screenshot tick) |
| e-stop applied before the step at tick 100 (v = 0.8) | v > 0 at tick 112, **v = 0 at tick 113** (13 steps). Distance 0.1843 m, below the bound v²/2·brake = 0.2 m. Mission `ABORTED` |
| same at tick 150, and at tick 300 (v = 0.7997) | rest at tick 163 and at tick 313, distances 0.1843 and 0.1842 m |
| last heartbeat applied at tick 195 | `linkOk` at tick 208, **lost at tick 209**, `PAUSED(LINK_LOST)`, rest at tick 221 |

A SHA-256 over the canonical trace (one `Locale.ROOT` line per tick, `%d %.6f %.6f %.6f %.6f` for tick, x, y, θ and v, `\n` line endings, UTF-8) was stable across runs. The build stores the trace itself as `golden/demo_trace.txt` and compares it line by line.

## 6. Wire protocol v1 (`:link`, original)

The protocol uses UTF-8 JSON text frames, one object per frame, at most 8 KiB per frame. The codec rejects unknown keys and any version other than 1.

- **Client → robot:** `{"v":1,"id":<int>,"type":…}`, where `type` is one of:
  - `hello`;
  - `heartbeat`;
  - `start` (with `missionId`, `name` and `waypoints: [[x,y],…]`);
  - `pause`, `resume`, `cancel`;
  - `estop`, `reset_estop`;
  - `step` (accepted only in lockstep mode).
- **Robot → client:**
  - `welcome {"v":1,"tickMs":40,"mode":"realtime"|"lockstep"}`;
  - `snapshot {"v":1,"tick",x,y,theta,speed,battery,"mission","pause","wp","estop","link","missionId"}`, one per tick;
  - `ack {"v":1,"id","result":"NONE"|<Reject>,"tick"}`, one per command, where `tick` is the tick the command was applied before;
  - `error {"v":1,"code":"BAD_MESSAGE"|"BUSY"}`.
- **Bad input.** Bad JSON, an unknown type, a non-finite number or an oversized frame gets `error BAD_MESSAGE`, and the simulator state does not change.

`RobotHost` (`:sim-server`) owns **one long-lived robot**, independent of any socket:

- **Single owner.** One coroutine serializes every command and step through a channel.
- **Sessions.**
  - At most one controlling session. A second connection gets `error BUSY` and is closed.
  - When the client disconnects, the robot keeps ticking. Its heartbeats stop, so the link-lost pause happens on its own.
  - A client that reconnects gets the current state (`welcome`, then the latest snapshot), including the latch.
- **Back-pressure.** Outbound frames go through a bounded buffer of 64 frames that keeps only the latest snapshots, so a slow socket never delays a tick.
- **Network.** The server binds `127.0.0.1` only.
- **Modes.**
  - Realtime mode ticks with `delay(40)`.
  - Lockstep mode, for tests and `--lockstep`, steps once per `step` message.

`Transport` interface (`:link`):

```kotlin
interface Transport {
    val snapshots: StateFlow<Telemetry?>
    val events: Flow<LinkEvent>          // Ack, Error, Connected, Disconnected
    suspend fun send(cmd: Command): Long // returns the command id
    fun close()
}
```

`InProcessTransport(sim, clock: TickSource)` runs the simulator in-process. `TickSource` is either a coroutine ticker on an injected dispatcher, or manual. `WebSocketTransport(url, HttpClient(CIO))` reconnects after 1 s and replays the latest snapshot to new collectors.

`HeartbeatSender` is owned by the **app**, not by the simulator. It sends `Heartbeat` every 200 ms while the app is in the foreground (`repeatOnLifecycle(STARTED)`). A backgrounded app stops sending heartbeats, and the robot then pauses with `LINK_LOST`. This is intended, and the README explains it.

## 7. App (`:app`)

- **Package.** `applicationId` and `namespace` are `com.applycoding.robot_companion`.
- **Permissions and network.** Permission `INTERNET`. A debug-only network security config allows cleartext only to `127.0.0.1` and `10.0.2.2`. The README covers `adb reverse tcp:8787 tcp:8787` for a physical device.
- **ViewModel.** `RobotViewModel(savedStateHandle, transportFactory)` exposes `StateFlow<UiState>` and has one `onEvent(UiEvent)` entry point. All composables are stateless, with state hoisted. The screen collects state with `collectAsStateWithLifecycle`.
- **Screen sections.**
  - The banner "SIMULATION ONLY, not a certified safety system".
  - A telemetry bar: tick, pose, speed, battery, link chip and mission state.
  - The map panel: room, waypoints, path, robot and heading.
  - The mission list: name, waypoint count, selected, last result.
  - A mission editor: name plus numeric waypoint rows. It validates on the client with the same bounds, but the robot remains the authority.
  - The control row: Start, Pause, Resume, Cancel, Reset e-stop.
  - A full-width E-STOP button.
  - A snackbar for `ack` rejects, showing the reject code.
- **E-STOP button.** Always enabled. Sends `EStop` on every press.
- **Reset button.** Enabled only when the latest snapshot has the latch set, `speed == 0` and `link == true`. The robot still checks again.
- **Saved state (process death).** `SavedStateHandle` stores:
  - the mission catalog (JSON, at most 16 missions);
  - the selected mission id;
  - the editor draft;
  - the transport mode.

  It never stores pose, latch or running state. After a restore, the in-process robot starts fresh at tick 0. A remote robot is re-attached and its state adopted. Nothing restarts automatically.
- **Transport mode.** A settings toggle switches between "In-app simulator" (the default) and "Sim server" (`ws://127.0.0.1:8787/v1/robot`). There are no build flavors.

## 8. Tests (every assertion is at a fixed tick or on an exact value; no window min/max, no `assumeTrue`, no `@Ignore`)

- **`:sim`.**
  - Every row of the section 5 measured table.
  - Each reject code in its precedence order.
  - Non-finite and out-of-room waypoints.
  - Idempotent e-stop.
  - A reset on a clear latch.
  - Heartbeat age 12, 13 and 14 (OK, OK, lost).
  - Low-battery pause, using a config with `startBatteryPct = 20.5` and `drainMovingPctPerTick = 0.05`; the exact pause tick is measured during the build and frozen.
  - The golden trace.
- **`:link`.**
  - `WireCodecTest` round-trips one golden JSON file per message type, with `estop: true` where it applies, and checks every rejection (unknown key, version, size, NaN).
  - `InProcessTransportTest` and `HeartbeatSenderTest` use `runTest` with a shared `TestCoroutineScheduler`, so 200 ms means exactly 5 ticks.
- **`:sim-server`.**
  - **`LockstepLiveTest`.** Real server on port 0, real `WebSocketTransport` (CIO) over localhost. It runs the scripted sequence: start at tick 0, heartbeat every 5 ticks, e-stop before tick 101. The 130 decoded snapshots must be **exactly equal** to an in-process reference run (measured: equal; rest at tick 113, mission `ABORTED`).
  - **`RealtimeLiveTest`.** Real realtime server and client. The test passes only if all of these hold:
    - decoded ticks strictly increase and reach ≥ 50 within a 10 s wall-clock deadline (a named failure otherwise);
    - the client sends `estop` on the first decoded snapshot with tick in [60, 180] and `speed == 0.8` (the first straight leg at full speed), and the `ack` for it reports tick `k`;
    - the snapshot at tick `k + 12` has `speed > 0`, and the one at `k + 13` has `speed == 0`, the latch set and mission `ABORTED`.

    It always closes the client and the server in `finally`.
  - **`SessionTest`.** A second client gets `BUSY`. After a disconnect, the robot is paused with `LINK_LOST` at the measured tick. A reconnect sees the latch still set.
- **`:app` (Robolectric `@Config(sdk = [37], qualifiers = "w360dp-h800dp-xxhdpi")`, `@GraphicsMode(NATIVE)`).**
  - **`ControlsUiTest`.**
    - It uses `InProcessTransport` with a manual tick source and composes the real `CompanionApp`, which collects through `collectAsStateWithLifecycle`.
    - The steps: step to tick 100, click E-STOP, step 13 ticks, then `waitForIdle()`.
    - It asserts that the rendered button reads `E-STOP LATCHED`, the status shows `tick 113` and `ABORTED`, and Reset is enabled.
    - It also asserts that Start is rejected with the `ESTOP_LATCHED` snackbar.
    - The prototype measured this flow: tick 113, `v = 0`, `ABORTED`, rendered `E-STOP LATCHED`.
  - **`ProcessDeathTest`.**
    - It saves through a real `SavedStateRegistryController` into a `Bundle`, parcels and unparcels it, and restores into a fresh owner with a fresh robot.
    - It asserts the catalog, the selection and the draft, plus `tick 0`, no latch, and mission `IDLE`.
  - **`ScreenshotTest`.**
    - It captures Roborazzi frames of the full app, with `RoborazziOptions(compareOptions = CompareOptions(changeThreshold = 0f))`, at these simulator states:
      - tick 0 with the demo selected;
      - ticks 150 and **300 (mid-mission, `RUNNING`, pose 8.44, 2.54)** and 450;
      - tick 627, `COMPLETED`;
      - tick 313 after an e-stop at tick 300 (latched).
    - Each image is asserted to be exactly **1080 × 2400** pixels after it is read with `ImageIO`. Measured: 1080×2400, and two fresh-process captures were byte-identical.
    - Goldens are under `app/src/test/screenshots/`. CI runs `verifyRoborazziDebug`.

## 9. Mutants (`tools/mutants.py`)

Each mutant is one exact-string patch.

1. The search string must occur exactly once, otherwise it is an error.
2. The patch is applied in a temporary copy of the repo.
3. The owning module must **compile**. A compile failure is an error, not a kill.
4. Only the target test runs, with `--rerun-tasks` scoped to the module.
5. A mutant counts as killed only if exactly that test fails, per the JUnit XML.

The runner first checks that every target passes on clean sources. Then it prints a kill matrix, and it fails unless all mutants are killed.

| Id | Patch | Target |
| --- | --- | --- |
| S1 | `brake` 1.6 → 0.4 | `EStopTest.restsAtTick113` |
| S2 | `heartbeatTimeoutTicks` 13 → 130 | `HeartbeatTest.lostAtTick209` |
| S3 | reset ignores `v ≠ 0` | `MissionRulesTest.resetRejectedWhileMoving` |
| S4 | the `ESTOP_LATCHED` check removed from start | `MissionRulesTest.startRejectedWhileLatched` |
| S5 | anticipatory braking removed | `KinematicsTest.demoCompletesAtTick627` |
| S6 | non-finite check removed | `MissionRulesTest.nonFiniteWaypointRejected` |
| L1 | server drops `estop` commands | `LockstepLiveTest.traceEqualsInProcess` |
| L2 | codec always encodes `estop` as false | `WireCodecTest.snapshotGolden` |
| L3 | realtime loop skips `step()` | `RealtimeLiveTest.ticksAdvance` (fails by deadline) |
| A1 | E-STOP button sends `Pause` | `ControlsUiTest.estopLatchesAndRenders` |
| A2 | the robot dot's radius changes by 2 px | `ScreenshotTest.midMission` (verify) |
| A3 | the screen reads a snapshot taken once (no collection) | `ControlsUiTest.estopLatchesAndRenders` (stale "E-STOP") |
| A4 | the catalog is not written to `SavedStateHandle` | `ProcessDeathTest.catalogSurvives` |

S1–S4, L1, A1 and A2 were prototyped: each compiled and was killed by exactly its target. S5, S6, L2, L3, A3 and A4 are specified but were not prototyped.

## 10. CI (`.github/workflows/ci.yml`)

The workflow runs on `push` and `pull_request`, with `permissions: contents: read`. All jobs run on `ubuntu-24.04` with Temurin 21.0.13 and `setup-gradle`. There is no `continue-on-error`, no `if: false`, and no job that only echoes text.

1. **`hygiene`.**
   - Wrapper validation.
   - `tools/check_spdx.py`. Every first-party file needs an SPDX header:
     - `//` comments in `.kt` and `.kts`;
     - `#` comments in `.toml`, `.properties` and `.yml`;
     - an XML comment after the declaration in `.xml`;
     - a `.license` sidecar for each PNG golden.
   - The LICENSE sha256 check.
   - A grep that fails on any `uses:` line without a 40-hex SHA.
   - A grep that fails on `@Ignore`, `assumeTrue` or `assertTrue(true)` in tests.
2. **`jvm`.** `./gradlew :sim:test :link:test :sim-server:test`.
3. **`android`.**
   - `sdkmanager --install "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"`.
   - `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug :app:assembleDebug`.
   - `tools/check_apk.sh`. It uses `aapt2 dump badging` and requires package `com.applycoding.robot_companion`, `application-debuggable`, `minSdkVersion 26` and `targetSdkVersion 37`. It fails if the APK file is missing.
   - It uploads the artifact `app-debug-apk`.
   - It records the frames, runs `tools/build_pages.py` (which writes `index.html` with the frames, their ticks and the run link, and fails if any of the 6 frames is missing or is not 1080×2400), and uploads the artifact `pages-site`.
4. **`mutants`.** `python tools/mutants.py`.
5. **`notice`.**
   - `./gradlew generateLicenseReport` for `:app` `debugRuntimeClasspath` (shipped in the APK) and for `:sim-server` `runtimeClasspath`.
   - `tools/check_notice.py` then compares `NOTICE` with the reports plus a curated build-and-test tools section that lists versions and licenses for:
     - the JDK, Gradle, AGP and Kotlin;
     - Robolectric, Roborazzi, JUnit and androidx.test;
     - the Android SDK platform and build-tools (Android Software Development Kit License, used to build and not redistributed).
   - It fails on a missing entry, an extra entry, or a dependency without license metadata, unless there is a reviewed override file entry.

## 11. Pages (`.github/workflows/pages.yml`)

- **Trigger.** `on: workflow_run` (workflows `[ci]`, `types: [completed]`).
- **Deploy job condition.** All of these must hold:
  - `github.event.workflow_run.conclusion == 'success'`;
  - `event == 'push'`;
  - `head_branch == 'main'`;
  - `head_repository.full_name == github.repository`.
- **Permissions.** `actions: read`, `pages: write`, `id-token: write`. Environment `github-pages`. Concurrency group `pages` with `cancel-in-progress: false`.
- **Steps.**
  1. If `workflow_run.head_sha` is not the current `main` HEAD (`gh api`), stop with a logged notice. This stops an older run from overwriting a newer one.
  2. Download `pages-site` with `run-id: ${{ github.event.workflow_run.id }}` and `github-token`. Do not rebuild or retest.
  3. `configure-pages`, `upload-pages-artifact` and `deploy-pages`.
  4. Smoke check with `curl`: the page returns 200, contains the banner text, and the mid-mission PNG returns 200.

The page shows the 6 frames with their ticks and states, the label "Simulation only, not a certified safety system", "debug build", and a link to the CI run where the `app-debug-apk` artifact can be downloaded.

## 12. Numbered success criteria (each mapped in `docs/verification.md`)

1. Every section-5 measured row is an exact fixed-tick assertion and passes, including the golden trace. Check: `:sim:test`, CI `jvm`.
2. The reject precedence and edge rules in section 5 each have a test that passes. Check: `MissionRulesTest`.
3. Codec goldens round-trip, and malformed input is rejected without changing state. Check: `WireCodecTest`.
4. `LockstepLiveTest` has 130 snapshots over a real socket, exactly equal to the in-process reference, with rest at tick 113. Check: CI `jvm`.
5. `RealtimeLiveTest`: ticks advance to at least 50 within 10 s. An e-stop sent at full speed leaves `speed > 0` at `ack.tick + 12` and `speed == 0` with the latch set at `ack.tick + 13`. Check: CI `jvm`.
6. `SessionTest`: a second client gets `BUSY`, a disconnect leads to `LINK_LOST`, and a reconnect still shows the latch. Check: CI `jvm`.
7. `ControlsUiTest`: the rendered state after an e-stop at tick 100 shows `E-STOP LATCHED`, `tick 113`, `ABORTED` and the reject snackbar. Check: CI `android`.
8. `ProcessDeathTest`: the catalog, selection and draft survive a parcelled Bundle, and the robot state does not. Check: CI `android`.
9. Six Roborazzi frames are exactly 1080×2400, the mid-mission frame is at tick 300 with `RUNNING`, and verification passes at threshold 0. Check: CI `android`.
10. The debug APK has package `com.applycoding.robot_companion`, is debuggable, has minSdk 26 and targetSdk 37, and is uploaded. Check: `check_apk.sh`.
11. All 13 mutants compile and are killed by exactly their targets, and the clean baseline passes. Check: CI `mutants`.
12. No ignored, assumed, skipped or always-true tests. Check: the `hygiene` grep, plus Gradle test reports showing 0 skipped.
13. `NOTICE` matches the license reports plus the curated tools section and points to `REFERENCES.md`. Check: CI `notice`.
14. Every first-party source file has an SPDX header, including `MainActivity.kt`, the manifests and the build scripts. Check: `check_spdx.py`.
15. `LICENSE` is byte-identical to the Apache-2.0 text (sha256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`). Check: `hygiene`.
16. Every action is pinned by SHA and the wrapper is validated. Check: `hygiene`.
17. Pages deploys only the `pages-site` artifact from a successful `push` run of ci on `main` in this repository, using the triggering run's id, with `actions: read`. The smoke check passes. Check: `pages.yml`, plus the first deploy after merge.
18. The README has:
    - the label "Simulation only, not a certified safety system" at the top;
    - "debug build, no Play Store, no iOS";
    - how to run the app and the server;
    - the safety rules with the measured ticks;
    - the backgrounding caveat;
    - "How this was verified";
    - "original implementation, inspired by", with links to `REFERENCES.md`;
    - honest limits.

    Check: review plus a grep for the label.

## 13. Why Robolectric and not an emulator in CI

The JVM path was proven here:

- Robolectric 4.17 on SDK 37 runs the real Compose UI test library and Roborazzi.
- It rendered a 1080×2400 native-graphics frame twice, byte-identical.
- It drove a click through to a rendered state change.

It needs no KVM, no system image, no boot wait and no ADB, and it is exact at the pixel level.

An emulator job (Gradle Managed Devices or `android-emulator-runner`) would add Android fidelity. But it also adds image downloads, KVM setup, boot time and graphics variability, and it would make exact screenshots harder, not easier. An emulator could not be tested on this machine either: `/dev/kvm` is not accessible and there is no Docker. So the plan does not include an emulator job.

The trade-off is that nothing in CI runs the APK on a real Android runtime. The README says so plainly.

## 14. Honest limits (the README must say these)

- This is a simulation, not a certified safety system. The e-stop is a software latch in a simulator.
- Debug APK only, never tested on an emulator or a device in CI. No release build, no store listing, no iOS.
- The screenshots are rendered by Robolectric's native graphics on the JVM, not on a device.
- The protocol has no authentication, and the server binds to loopback only.
- `SavedStateHandle` covers state restored by the system after process death. It does not cover force-stop or permanent storage.
- When the app is in the background it stops sending heartbeats, so a running mission pauses with `LINK_LOST`.

## 15. Risks

- **Screenshot pixels differ between machines.** The goldens must be recorded on the CI image (ubuntu-24.04 x86_64, Temurin 21.0.13, `Locale.ROOT`/`en-US`, UTC, font scale 1). If the first CI verification fails only because of the environment, commit the actual images that CI produced for that run, and record this in `DEVIATIONS.md`. Never raise the threshold.
- **The SDK 37 test path depends on the explicit espresso 3.7.0 and test core 1.7.0 pins** and on the JVM arguments in section 3.
- **The Ktor CIO WebSocket client has only been tested on the JVM.** On Android, it is covered only by compilation.

## 16. License and authorship

- License: Apache-2.0. Every commit's author and committer is Arpan Bom <applycoding@gmail.com>, with no trailers.
- References are idea-only. See `REFERENCES.md`.
