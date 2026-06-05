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

See **[RELEASE.md](RELEASE.md)** for the full checklist.

### Critical rule: hash the live GitHub asset

`expectedSha256` in `release/version.json` must be the SHA256 of the APK **as served from GitHub** after upload — not the local build artifact.

Local `assembleDebug` output and CI `release-apk.yml` output can differ in size and hash even with the same keystore. In-app updates download from GitHub and compare against `expectedSha256`; a local hash causes `APK hash mismatch` for users.

**Required order:**

1. Publish the GitHub release (`sway_meditation.apk` at `/releases/download/vX.Y.Z/`)
2. Download that URL (or use `gh release download`)
3. Compute SHA256 of the downloaded file
4. Write `release/version.json` with pinned `apkUrl` and that hash
5. Run `.\release\verify-manifest.ps1` — must pass before merging to `main`

### Recommended: publish script

From repo root:

```powershell
.\release\publish-release.ps1 -ReleaseNotes "• your release bullets"
```

The script builds (optional), uploads via `gh`, downloads the live asset, writes `version.json`, and runs the verifier.

### Manual build (debug sideload)

```powershell
.\gradlew.bat assembleDebug
```

Gradle outputs `app/build/outputs/apk/debug/app-debug.apk`. Upload as **`sway_meditation.apk`** on the GitHub release.

Use a version-pinned `apkUrl` (`/releases/download/vX.Y.Z/sway_meditation.apk`), not `/releases/latest/download/`.

### Verify before merge

```powershell
.\release\verify-manifest.ps1
```

CI runs the same check when `release/version.json` changes (`.github/workflows/ci.yml`).

Optional local signing check (not a substitute for live hash):

```powershell
$apksigner = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Recurse -Filter "apksigner.bat" | Sort-Object FullName -Descending | Select-Object -First 1
& $apksigner.FullName verify --print-certs app\build\outputs\apk\debug\app-debug.apk
```

Expected certificate DN: `CN=Serene Interval Sideload, OU=Updates, O=Serene Interval, C=US`
