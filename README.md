# SPDX-License-Identifier: Apache-2.0

# compose-android-robot-companion

**Simulation only, not a certified safety system.** This repository ships a **debug build** only. No Play Store listing and no iOS target.

Native Kotlin and Jetpack Compose companion for one **simulated** robot: live telemetry, mission catalog, and a latched e-stop. Safety rules are enforced in the simulator on the robot side.

Original implementation, inspired by the public work listed in [REFERENCES.md](REFERENCES.md).

## Run

```bash
./gradlew :app:assembleDebug
./gradlew :sim-server:run --args='--port=8787'
```

On a physical device, expose the server: `adb reverse tcp:8787 tcp:8787`, then switch the in-app transport to **Sim server** (`ws://127.0.0.1:8787/v1/robot`).

## Safety rules (measured ticks)

Demo mission with heartbeat every 5 ticks:

- `v = vMax` at tick **40**
- Waypoint index 1 at **237**, index 2 at **407**
- Mission completes at **627** (running at **626**)
- E-stop before step at tick **100**: rest at **113**, mission **ABORTED**
- Last heartbeat at tick **195**: link lost at **209**, pause, rest at **221**
- Mid-mission screenshot pose at tick **300**: (8.44, 2.54), `RUNNING`

## Backgrounding

The app sends heartbeats only while in the foreground (`STARTED`). When backgrounded, heartbeats stop and a running mission pauses with `LINK_LOST` until the operator returns.

## Limits

- Simulation only; not certified machinery safety
- Debug APK; not exercised on a physical device in CI
- Roborazzi frames are rendered on the JVM (Robolectric native graphics)
- WebSocket protocol has no authentication; server binds to loopback
- `SavedStateHandle` restores UI catalog after process death, not force-stop
- No release signing, R8, Play Store, or iOS

## How this was verified

- JVM simulator tests for every measured tick in `PLAN.md`
- Live stack tests against `:sim-server` with real Ktor WebSocket transport
- Robolectric + Compose UI tests and Roborazzi screenshots (threshold 0)
- Thirteen compile-checked mutants (`tools/mutants.py`)
- CI workflow `ci` (`hygiene`, `jvm`, `android`, `mutants`, `notice`) — see [docs/verification.md](docs/verification.md)

License: Apache-2.0. Author: Arpan Bom.
