# Sideload update signing

GitHub update APKs must be signed with the **same key** as the app already on the phone. Android rejects over-the-top installs when signatures differ (`App not installed`).

## Project keystore

All `assembleDebug` / `assembleRelease` builds use the shared sideload keystore:

- `keystore/sideload.jks`
- `keystore/sideload.properties`

This keystore is committed on purpose so every developer machine and CI produce matching APKs. It is **not** a Play Store release key.

## One-time fix for existing installs

If a phone was installed with a machine-specific Android debug key (default `~/.android/debug.keystore` on another PC):

1. Uninstall Sway Meditation on the device.
2. Install the latest APK from [GitHub Releases](https://github.com/mpburton812/serene-interval/releases) (or run `installDebug` after pulling this change).
3. Future in-app updates will install normally.

## Publish a new release APK

From repo root:

```powershell
.\gradlew.bat assembleDebug
```

Gradle still outputs `app/build/outputs/apk/debug/app-debug.apk` locally. For GitHub releases, upload it as **`sway_meditation.apk`** (the release workflow renames automatically).

**Order matters for in-app updates:** publish the GitHub release first, then update `release/version.json` on `main`. Use a version-pinned `apkUrl` (`/releases/download/vX.Y.Z/sway_meditation.apk`), not `/releases/latest/download/`, so the manifest never points at a newer hash while GitHub still serves the previous release. List the correct `versionCode`, `versionName`, and `expectedSha256` (hash the uploaded APK before committing).

Verify signing before upload:

```powershell
$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "apksigner.bat" | Sort-Object FullName -Descending | Select-Object -First 1
& $apksigner.FullName verify --print-certs app\build\outputs\apk\debug\app-debug.apk
```

Expected certificate DN: `CN=Serene Interval Sideload, OU=Updates, O=Serene Interval, C=US`
