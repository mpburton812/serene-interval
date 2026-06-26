#!/usr/bin/env bash
# Preflight checks for Android builds/tests. Every wait has a hard timeout.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ADB_TIMEOUT_SEC="${ADB_TIMEOUT_SEC:-30}"
BOOT_TIMEOUT_SEC="${BOOT_TIMEOUT_SEC:-120}"
BOOT_POLL_SEC="${BOOT_POLL_SEC:-2}"
RESPONSIVE_TIMEOUT_SEC="${RESPONSIVE_TIMEOUT_SEC:-10}"
REQUIRE_DEVICE=0
REQUIRE_RESPONSIVE=0
REQUIRE_AVD_NAME=""
SERIAL="${ANDROID_SERIAL:-}"

usage() {
    cat <<'EOF'
Usage: scripts/verify-android-env.sh [options]

Options:
  --device                 Require a connected adb device
  --responsive             Require adb shell to respond within 10s (use with --device)
  --avd <name>             Require running emulator AVD name (case-insensitive)
  --serial <id>            adb serial (default: ANDROID_SERIAL or sole device)
  --adb-timeout <sec>      adb wait-for-device timeout (default: 30)
  --boot-timeout <sec>     sys.boot_completed wait timeout (default: 120)
  -h, --help               Show this help

Exits non-zero on timeout or failed check. Prints actionable errors.
EOF
}

die() {
    echo "verify-android-env: $*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "missing command: $1"
}

resolve_android_home() {
    if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
        echo "$ANDROID_HOME"
        return
    fi
    if [[ -f "$REPO_ROOT/local.properties" ]]; then
        local sdk_dir
        sdk_dir="$(grep -E '^sdk\.dir=' "$REPO_ROOT/local.properties" | head -1 | cut -d= -f2- | tr -d '\r')"
        if [[ -n "$sdk_dir" && -d "$sdk_dir" ]]; then
            echo "$sdk_dir"
            return
        fi
    fi
    die "ANDROID_HOME not set and sdk.dir missing in local.properties"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --device) REQUIRE_DEVICE=1; shift ;;
        --responsive) REQUIRE_RESPONSIVE=1; REQUIRE_DEVICE=1; shift ;;
        --avd) REQUIRE_AVD_NAME="${2:-}"; REQUIRE_DEVICE=1; shift 2 ;;
        --serial) SERIAL="${2:-}"; shift 2 ;;
        --adb-timeout) ADB_TIMEOUT_SEC="${2:-}"; shift 2 ;;
        --boot-timeout) BOOT_TIMEOUT_SEC="${2:-}"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) die "unknown option: $1" ;;
    esac
done

ANDROID_HOME="$(resolve_android_home)"
export ANDROID_HOME
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

require_command adb

if [[ ! -f "$REPO_ROOT/local.properties" ]]; then
    die "local.properties missing — run: echo sdk.dir=$ANDROID_HOME > local.properties"
fi

echo "OK  sdk: $ANDROID_HOME"

if [[ "$REQUIRE_DEVICE" -eq 0 ]]; then
    echo "verify-android-env: device checks skipped (pass --device to require emulator)"
    exit 0
fi

if [[ -n "$SERIAL" ]]; then
    export ANDROID_SERIAL="$SERIAL"
    echo "OK  serial: $ANDROID_SERIAL"
fi

echo "verify-android-env: waiting for adb device (timeout ${ADB_TIMEOUT_SEC}s)..."
if ! timeout "$ADB_TIMEOUT_SEC" adb wait-for-device 2>/dev/null; then
    die "adb wait-for-device timed out after ${ADB_TIMEOUT_SEC}s. Is the emulator running?"
fi

STATE="$(adb get-state 2>/dev/null || true)"
[[ "$STATE" == "device" ]] || die "adb device state is '$STATE' (expected device)"

MODEL="$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
AVD_NAME="$(adb shell getprop ro.boot.qemu.avd_name 2>/dev/null | tr -d '\r' || true)"
echo "OK  device: ${MODEL:-unknown} avd=${AVD_NAME:-n/a}"

if [[ -n "$REQUIRE_AVD_NAME" ]]; then
    expected="$(echo "$REQUIRE_AVD_NAME" | tr '[:upper:]' '[:lower:]')"
    actual="$(echo "$AVD_NAME" | tr '[:upper:]' '[:lower:]')"
    [[ "$actual" == "$expected" ]] || die "expected AVD '$REQUIRE_AVD_NAME' but running '$AVD_NAME'"
    echo "OK  avd name matches: $AVD_NAME"
fi

echo "verify-android-env: waiting for boot (timeout ${BOOT_TIMEOUT_SEC}s)..."
deadline=$((SECONDS + BOOT_TIMEOUT_SEC))
while (( SECONDS < deadline )); do
    boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$boot" == "1" ]]; then
        echo "OK  boot completed"
        break
    fi
    sleep "$BOOT_POLL_SEC"
done

if [[ "${boot:-}" != "1" ]]; then
    die "sys.boot_completed not ready after ${BOOT_TIMEOUT_SEC}s"
fi

if [[ "$REQUIRE_RESPONSIVE" -eq 1 ]]; then
    echo "verify-android-env: checking adb shell responsiveness (timeout ${RESPONSIVE_TIMEOUT_SEC}s)..."
    if ! timeout "$RESPONSIVE_TIMEOUT_SEC" adb shell echo ok 2>/dev/null | grep -q ok; then
        die "adb shell not responsive within ${RESPONSIVE_TIMEOUT_SEC}s — emulator may be hung; restart Sway_Test"
    fi
    echo "OK  adb shell responsive"
fi

exit 0
