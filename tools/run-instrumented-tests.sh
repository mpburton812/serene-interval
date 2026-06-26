#!/usr/bin/env bash
# Builds and runs Espresso / Compose instrumented tests on a connected device or emulator.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if [[ -z "${ANDROID_HOME:-}" ]]; then
  if [[ -f local.properties ]]; then
    SDK_DIR="$(grep -E '^sdk\.dir=' local.properties | cut -d= -f2- | tr -d '\r')"
    if [[ -n "$SDK_DIR" ]]; then
      export ANDROID_HOME="$SDK_DIR"
    fi
  fi
fi

if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/Android/Sdk" ]]; then
  export ANDROID_HOME="$HOME/Android/Sdk"
fi

export PATH="${ANDROID_HOME:-}/platform-tools:${ANDROID_HOME:-}/emulator:${ANDROID_HOME:-}/cmdline-tools/latest/bin:$PATH"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Set ANDROID_HOME or sdk.dir in local.properties." >&2
  exit 1
fi

wait_for_device() {
  local timeout="${1:-120}"
  local elapsed=0
  while (( elapsed < timeout )); do
    if adb get-state 2>/dev/null | grep -q device; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  return 1
}

start_emulator_if_needed() {
  if adb devices | awk 'NR>1 && $2=="device" { found=1 } END { exit !found }'; then
    echo "Using connected device/emulator."
    return 0
  fi

  if ! command -v emulator >/dev/null 2>&1; then
    echo "No device connected and emulator binary not installed." >&2
    echo "Install with: sdkmanager emulator \"system-images;android-36;google_apis;x86_64\"" >&2
    exit 1
  fi

  local avd_name="serene_test_avd"
  if ! avdmanager list avd | grep -q "Name: $avd_name"; then
    echo "Creating AVD $avd_name ..."
  echo no | avdmanager create avd \
      -n "$avd_name" \
      -k "system-images;android-36;google_apis;x86_64" \
      -d "pixel_6" \
      --force
  fi

  echo "Starting emulator $avd_name ..."
  emulator -avd "$avd_name" -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
  if ! wait_for_device 180; then
    echo "Emulator failed to boot within 180s." >&2
    exit 1
  fi
  adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done; input keyevent 82' || true
}

start_emulator_if_needed

echo "Building instrumented test APK ..."
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --no-daemon

echo "Installing APKs ..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

echo "Running connectedAndroidTest ..."
./gradlew :app:connectedDebugAndroidTest --no-daemon

echo "Reports: app/build/reports/androidTests/connected/debug/index.html"
