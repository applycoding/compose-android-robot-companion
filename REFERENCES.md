# References: compose-android-robot-companion

These references are for ideas and documentation only. No source, test, figure or text from them is copied, translated or vendored into this repo.

Licenses were checked on 2026-11-10. The outbound license for this repo is Apache-2.0. Libraries that are actually used as dependencies are listed with versions and licenses in `NOTICE`.

## Not a duplicate of these applycoding repos

| Repo | Difference |
| --- | --- |
| [flutter-robot-operator](https://github.com/applycoding/flutter-robot-operator) | Flutter and Dart, with its own simulator, protocol, constants and browser demo. This repo is native Kotlin and Compose with a separate Kotlin simulator: 25 Hz, unicycle model with a heading-rate limit, e-stop aborts the mission, and heartbeat loss pauses it. There is no browser runtime. No code was copied, and the protocol is new. |
| [teleop-console-demo](https://github.com/applycoding/teleop-console-demo) | Browser velocity teleop with FastAPI. This repo is mission-level operation from an Android app. |
| [kmp-compose-swiftui-pos](https://github.com/applycoding/kmp-compose-swiftui-pos) | Compose in a point-of-sale app (Room, GraphQL). This repo uses Compose for an operator and safety UI with live coroutine telemetry. |
| [android-mvvm-retrofit-app](https://github.com/applycoding/android-mvvm-retrofit-app), [kotlin-ble-iot-companion](https://github.com/applycoding/kotlin-ble-iot-companion) | A REST client app and a read-only BLE sensor companion. Neither sends commands and neither has safety rules enforced on the robot side. |

## Idea references

### 1. Android architecture guidance

- Compose architecture and unidirectional data flow: https://developer.android.com/develop/ui/compose/architecture
- State hoisting: https://developer.android.com/develop/ui/compose/state-hoisting
- Saving UI state and `SavedStateHandle` guarantees: https://developer.android.com/topic/libraries/architecture/saving-states
- Compose testing: https://developer.android.com/develop/ui/compose/testing
- Ideas used: stateless composables, a single `StateFlow` UI state, and which state should survive process death.
- The documentation is Apache-2.0 for code samples and CC BY 4.0 for text, per the site footer.

### 2. Now in Android (android/nowinandroid)

- URL: https://github.com/android/nowinandroid (HEAD `2982e63cb691`, checked 2026-11-10). License: Apache-2.0.
- Ideas used: a version catalog and module boundaries, and JVM screenshot tests with Roborazzi in CI. No code was copied.

### 3. kotlinx.coroutines test documentation

- URL: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/. The project https://github.com/Kotlin/kotlinx.coroutines is Apache-2.0.
- Idea used: a virtual-time `TestCoroutineScheduler` shared between dispatchers. It is also a test dependency (see `NOTICE`).

### 4. Robolectric and Roborazzi

- Robolectric: https://robolectric.org/ and https://github.com/robolectric/robolectric (MIT).
- Roborazzi: https://github.com/takahirom/roborazzi (Apache-2.0).
- Idea used: Android UI and screenshot tests on the JVM with native graphics, so no emulator is needed. Both are test dependencies (see `NOTICE`).

### 5. Ktor WebSockets

- URL: https://ktor.io/docs/client-websockets.html. The project https://github.com/ktorio/ktor is Apache-2.0.
- Used as a dependency for the server and the client transport. The protocol in this repo is new.

### 6. Emergency stop concept (ISO 13850)

- ISO 13850:2015, "Safety of machinery — Emergency stop function — Principles for design". It is a paid standard and was not used as a source; only the title is cited here.
- Idea used: the general idea of a latched stop that must be reset deliberately and never restarts motion automatically. This repo is **not** designed, tested or certified to that standard or any other.
