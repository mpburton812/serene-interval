# AGENTS.md

## Cursor Cloud specific instructions

This repository is a single **Android app** (Jetpack Compose) — "Serene Interval" / Sway
(Gradle module `:app`, applicationId `com.example.meditationparticles`, root project
`MeditationParticles`). It is a meditation / breathing / affirmations app. There is no
backend or web service; the only artifact is the debug APK.

### Preinstalled toolchain (baked into the VM snapshot)

The following are already installed on the VM image; the startup update script does not
reinstall them:

- **JDK 17** — set as the system default `java`/`javac` (via `update-alternatives`). AGP
  9.2.0 requires JDK 17; do **not** switch the default to the also-present JDK 21.
- **Android SDK** at `~/android-sdk` (`platform-tools`, `platforms;android-36`,
  `build-tools;36.0.0`, `emulator`, `system-images;android-36;google_apis;x86_64`).
- Convenience env vars (`JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `PATH`) are
  exported from `~/.bashrc`. They are **not** required for builds — the default `java` is
  already 17 and `local.properties` (written by the update script) provides `sdk.dir` — but
  `adb`/`emulator`/`sdkmanager` are only on `PATH` after `source ~/.bashrc`.

### Build / lint / test

Standard commands (see also `scripts/run-tests.sh` and `.github/workflows/ci.yml`):

- Build debug APK: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Lint: `./gradlew lintDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- CI-equivalent unit + lint in one shot: `bash scripts/run-tests.sh --unit --lint`

The first Gradle invocation downloads the wrapper distribution and dependencies and can take
a few minutes. Debug builds are signed with the committed `keystore/sideload.jks` (see
`release/SIGNING.md`); no extra signing setup is needed.

### Running the app on the emulator (important caveats)

An AVD named `sway_test` (`system-images;android-36;google_apis;x86_64`) is preconfigured.
**This VM has no `/dev/kvm`**, so the emulator only runs in slow software (QEMU TCG) mode:

- Launch headless: `emulator -avd sway_test -no-accel -accel off -gpu swiftshader_indirect -no-snapshot -no-boot-anim -no-audio -no-window` (after `source ~/.bashrc`).
- Cold boot to `sys.boot_completed=1` takes several minutes; installing the ~130 MB debug
  APK also takes several minutes. Use generous timeouts.
- Right after boot the guest is heavily loaded (system dexopt) and `system_server`/SystemUI
  may throw "isn't responding" ANRs. Set `adb shell settings put global hide_error_dialogs 1`
  and give it time to settle; these are emulator-performance artifacts, not app crashes.
- `adb exec-out screencap` frequently returns an all-black frame under software rendering;
  it reliably captures content only immediately after an input-driven redraw
  (e.g. `input keyevent KEYCODE_WAKEUP` then `input swipe 540 2000 540 600 200`).
- **Compose touch input is unreliable** on this software emulator: because the Choreographer
  frame loop stalls when the screen is idle, `adb shell input tap/swipe/motionevent` events
  are often not processed by the app's Compose UI (system Views/dialogs still respond via
  key events). Do not rely on scripted `adb input` to drive the app's Compose screens here;
  prefer JVM unit tests / instrumented tests for behavior verification, or an emulator with
  hardware acceleration for interactive UI testing.

### Git workflow

Branch/release conventions are enforced by rules in `.cursor/rules/` (dev/test/main
promotion flow, release deploy gate). The release/manifest tooling under `release/` is
PowerShell-based and intended for Windows maintainers.
