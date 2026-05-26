#!/usr/bin/env bash
# Validates release/version.json and confirms apkUrl serves the expected SHA256.
set -euo pipefail

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing required command: $1" >&2
        return 1
    fi
}

validate_manifest_json() {
    local manifest_path="$1"

    if [[ ! -f "$manifest_path" ]]; then
        echo "Manifest not found: $manifest_path" >&2
        return 1
    fi

    VERSION_CODE="$(jq -r '.versionCode' "$manifest_path")"
    VERSION_NAME="$(jq -r '.versionName' "$manifest_path")"
    APK_URL="$(jq -r '.apkUrl' "$manifest_path")"
    EXPECTED_SHA256="$(jq -r '.expectedSha256 // empty' "$manifest_path")"

    if [[ -z "$VERSION_CODE" || "$VERSION_CODE" == "null" ]]; then
        echo "versionCode is required in $manifest_path" >&2
        return 1
    fi
    if [[ -z "$VERSION_NAME" || "$VERSION_NAME" == "null" ]]; then
        echo "versionName is required in $manifest_path" >&2
        return 1
    fi
    if [[ -z "$APK_URL" || "$APK_URL" == "null" ]]; then
        echo "apkUrl is required in $manifest_path" >&2
        return 1
    fi
    if [[ -z "$EXPECTED_SHA256" ]]; then
        echo "expectedSha256 is required in $manifest_path" >&2
        return 1
    fi

    local normalized_sha
    normalized_sha="$(echo "$EXPECTED_SHA256" | tr '[:upper:]' '[:lower:]' | tr -d '[:space:]')"
    if [[ ! "$normalized_sha" =~ ^[0-9a-f]{64}$ ]]; then
        echo "expectedSha256 must be 64 hex characters in $manifest_path" >&2
        return 1
    fi
    EXPECTED_SHA256="$normalized_sha"

    if [[ "$APK_URL" == *"/releases/latest/download/"* ]]; then
        echo "apkUrl must not use /releases/latest/download/ (pin to /releases/download/vX.Y.Z/)" >&2
        return 1
    fi

    local expected_tag="v${VERSION_NAME}"
    if [[ "$APK_URL" != *"/releases/download/${expected_tag}/"* ]]; then
        echo "apkUrl must include /releases/download/${expected_tag}/ for versionName ${VERSION_NAME}" >&2
        return 1
    fi

    if [[ "$APK_URL" != *"sway_meditation.apk" ]]; then
        echo "apkUrl must end with sway_meditation.apk" >&2
        return 1
    fi
}

verify_apk_hash() {
    local apk_file="$1"
    local actual_sha
    actual_sha="$(sha256sum "$apk_file" | awk '{print $1}' | tr '[:upper:]' '[:lower:]')"
    if [[ "$actual_sha" != "$EXPECTED_SHA256" ]]; then
        echo "APK hash mismatch." >&2
        echo "  expected: $EXPECTED_SHA256" >&2
        echo "  actual:   $actual_sha" >&2
        return 1
    fi
    echo "APK SHA256 verified: $actual_sha"
}

run_self_tests() {
    require_command jq
    require_command sha256sum

    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "$tmp_dir"' EXIT

    pass_manifest="$tmp_dir/pass.json"
    cat >"$pass_manifest" <<'EOF'
{
  "versionCode": 99,
  "versionName": "9.9.9",
  "apkUrl": "https://github.com/example/repo/releases/download/v9.9.9/sway_meditation.apk",
  "expectedSha256": "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789ab"
}
EOF

    validate_manifest_json "$pass_manifest"

    for bad_url in \
        "https://github.com/example/repo/releases/latest/download/sway_meditation.apk" \
        "https://github.com/example/repo/releases/download/v9.9.8/sway_meditation.apk"
    do
        bad_manifest="$tmp_dir/bad-url.json"
        jq --arg url "$bad_url" '.apkUrl = $url' "$pass_manifest" >"$bad_manifest"
        if validate_manifest_json "$bad_manifest" 2>/dev/null; then
            echo "Self-test failed: expected rejection for apkUrl=$bad_url" >&2
            return 1
        fi
    done

    bad_sha_manifest="$tmp_dir/bad-sha.json"
    jq '.expectedSha256 = "not-a-hash"' "$pass_manifest" >"$bad_sha_manifest"
    if validate_manifest_json "$bad_sha_manifest" 2>/dev/null; then
        echo "Self-test failed: expected rejection for invalid expectedSha256" >&2
        return 1
    fi

    test_apk="$tmp_dir/test.apk"
    printf 'test-apk-bytes' >"$test_apk"
    EXPECTED_SHA256="$(sha256sum "$test_apk" | awk '{print $1}')"
    if ! verify_apk_hash "$test_apk" >/dev/null; then
        echo "Self-test failed: hash verification should pass for matching APK" >&2
        return 1
    fi

    EXPECTED_SHA256="0000000000000000000000000000000000000000000000000000000000000000"
    if verify_apk_hash "$test_apk" 2>/dev/null; then
        echo "Self-test failed: hash verification should reject mismatch" >&2
        return 1
    fi

    echo "Self-tests passed."
}

main() {
    require_command jq || exit 1
    require_command sha256sum || exit 1

    validate_manifest_json "$MANIFEST" || exit 1

    if [[ -n "$LOCAL_APK" ]]; then
        if [[ ! -f "$LOCAL_APK" ]]; then
            echo "Local APK not found: $LOCAL_APK" >&2
            exit 1
        fi
        verify_apk_hash "$LOCAL_APK" || exit 1
        exit 0
    fi

    require_command curl || exit 1
    tmp_apk="$(mktemp -t sway-apk.XXXXXX.apk)"
    trap 'rm -f "$tmp_apk"' EXIT

    echo "Downloading $APK_URL ..."
    if ! curl -fsSL --retry 3 --retry-delay 2 -o "$tmp_apk" "$APK_URL"; then
        echo "Failed to download apkUrl. Publish the GitHub release before updating version.json." >&2
        exit 1
    fi

    verify_apk_hash "$tmp_apk" || exit 1
}

MANIFEST="release/version.json"
SELF_TEST=false
LOCAL_APK=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --self-test)
            SELF_TEST=true
            shift
            ;;
        --local-apk)
            LOCAL_APK="${2:-}"
            shift 2
            ;;
        *)
            MANIFEST="$1"
            shift
            ;;
    esac
done

if [[ "$SELF_TEST" == true ]]; then
    run_self_tests || exit 1
else
    main
fi
