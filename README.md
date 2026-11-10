# compose-android-robot-companion

**Simulation only, not a certified safety system.** Debug build only. No Play Store, no iOS.

A native Android companion app, written in Kotlin and Jetpack Compose, for one simulated robot. It provides:

- live telemetry;
- a mission list (create, select, start, pause, resume, cancel);
- a latched e-stop.

A deterministic pure-Kotlin simulator enforces every safety rule on the robot side. It runs either inside the app or behind a small WebSocket server. See `PLAN.md`.

Original implementation, inspired by the public work listed in `REFERENCES.md`.

License: Apache-2.0. Author: Arpan Bom.
