#!/usr/bin/env bash
# Run Android tests with preflight checks and hard timeouts (no infinite hangs).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

RUN_UNIT=0
RUN_CONNECTED=0
RUN_LINT=0
REQUIRE_AVD_NAME="${SWAY_TEST_AVD:-sway_test}"
SERIAL="${ANDROID_SERIAL:-}"
UNIT_TIMEOUT_SEC="${UNIT_TIMEOUT_SEC:-600}"
CONNECTED_TIMEOUT_SEC="${CONNECTED_TIMEOUT_SEC:-900}"
LINT_TIMEOUT_SEC="${LINT_TIMEOUT_SEC:-600}"
GRADLE_ARGS=(--no-daemon --stacktrace)

usage() {
    cat <<'EOF'
Usage: scripts/run-tests.sh [options]

Runs preflight checks, then Gradle tests with hard timeouts.

Options:
  --unit                   Run testDebugUnitTest (default if no mode given)
  --connected              Run connectedDebugAndroidTest (requires emulator)
  --lint                   Run lintDebug
  --all                    Unit + lint + connected tests
  --avd <name>             Required emulator AVD name (default: sway_test)
  --serial <id>            adb serial (default: ANDROID_SERIAL)
  --unit-timeout <sec>     Unit test timeout (default: 600)
  --connected-timeout <sec> Instrumentation timeout (default: 900)
  -h, --help               Show this help

Environment:
  ANDROID_HOME, ANDROID_SERIAL, SWAY_TEST_AVD

Examples:
  scripts/run-tests.sh --unit
  scripts/run-tests.sh --connected --avd sway_test
  ANDROID_SERIAL=emulator-5554 scripts/run-tests.sh --all
EOF
}

die() {
    echo "run-tests: $*" >&2
    exit 1
}

prepare_device_for_connected_tests() {
    local app_id="com.example.meditationparticles"
    local test_id="${app_id}.test"
    echo "run-tests: preparing device for instrumentation (timeouts enforced)..."

    timeout 30 adb uninstall "$test_id" >/dev/null 2>&1 || true
    timeout 30 adb uninstall "$app_id" >/dev/null 2>&1 || true

    local apk="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
    if [[ -f "$apk" ]]; then
        echo "run-tests: smoke-install debug APK (timeout 180s)..."
        if ! timeout 180 adb install -r -t "$apk" >/tmp/adb-install-smoke.log 2>&1; then
            echo "run-tests: smoke install failed — emulator may be stuck. Log:" >&2
            tail -20 /tmp/adb-install-smoke.log >&2 || true
            die "aborting connected tests before Gradle 6-minute install hang"
        fi
        echo "run-tests: smoke install OK"
    else
        echo "run-tests: debug APK not built yet; Gradle will build during connected tests"
    fi
}

run_with_timeout() {
    local label="$1"
    local seconds="$2"
    shift 2
    echo "run-tests: $label (timeout ${seconds}s)"
    if timeout "$seconds" "$@"; then
        echo "run-tests: $label passed"
    else
        local code=$?
        if [[ $code -eq 124 ]]; then
            die "$label timed out after ${seconds}s"
        fi
        die "$label failed (exit $code)"
    fi
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --unit) RUN_UNIT=1; shift ;;
        --connected) RUN_CONNECTED=1; shift ;;
        --lint) RUN_LINT=1; shift ;;
        --all) RUN_UNIT=1; RUN_CONNECTED=1; RUN_LINT=1; shift ;;
        --avd) REQUIRE_AVD_NAME="${2:-}"; shift 2 ;;
        --serial) SERIAL="${2:-}"; shift 2 ;;
        --unit-timeout) UNIT_TIMEOUT_SEC="${2:-}"; shift 2 ;;
        --connected-timeout) CONNECTED_TIMEOUT_SEC="${2:-}"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) die "unknown option: $1" ;;
    esac
done

if [[ "$RUN_UNIT" -eq 0 && "$RUN_CONNECTED" -eq 0 && "$RUN_LINT" -eq 0 ]]; then
    RUN_UNIT=1
fi

cd "$REPO_ROOT"

VERIFY_ARGS=()
if [[ "$RUN_CONNECTED" -eq 1 ]]; then
    VERIFY_ARGS+=(--device --responsive --avd "$REQUIRE_AVD_NAME")
    [[ -n "$SERIAL" ]] && VERIFY_ARGS+=(--serial "$SERIAL")
else
    # SDK-only check for JVM unit tests / lint
    VERIFY_ARGS=()
fi

if [[ ${#VERIFY_ARGS[@]} -gt 0 ]]; then
    bash "$SCRIPT_DIR/verify-android-env.sh" "${VERIFY_ARGS[@]}"
else
    # Still verify SDK exists
    bash "$SCRIPT_DIR/verify-android-env.sh"
fi

if [[ "$RUN_UNIT" -eq 1 ]]; then
    run_with_timeout "unit tests" "$UNIT_TIMEOUT_SEC" \
        ./gradlew testDebugUnitTest "${GRADLE_ARGS[@]}"
fi

if [[ "$RUN_LINT" -eq 1 ]]; then
    run_with_timeout "lint" "$LINT_TIMEOUT_SEC" \
        ./gradlew lintDebug "${GRADLE_ARGS[@]}"
fi

if [[ "$RUN_CONNECTED" -eq 1 ]]; then
    if [[ -n "$SERIAL" ]]; then
        export ANDROID_SERIAL="$SERIAL"
    fi
    prepare_device_for_connected_tests
    run_with_timeout "connected tests" "$CONNECTED_TIMEOUT_SEC" \
        ./gradlew connectedDebugAndroidTest "${GRADLE_ARGS[@]}"
fi

echo "run-tests: all requested checks passed"
