# Release checklist

Use this for every sideload release. Skipping a step causes in-app update hash mismatches.

## Before you start

- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Update `release/CHANGELOG.md`
- [ ] `gh auth status` succeeds (needed for publish script and reliable downloads)

## Publish (required order)

1. **Build** — `.\gradlew.bat assembleDebug` (or let `publish-release.ps1` build)
2. **Upload** — create/update the GitHub release with `sway_meditation.apk`
3. **Download back** — fetch the APK from the **release URL**, not from `app/build/outputs/`
4. **Hash live bytes** — SHA256 of the downloaded file becomes `expectedSha256`
5. **Write manifest** — update `release/version.json` on `main` with pinned `apkUrl` and that hash
6. **Verify** — `.\release\verify-manifest.ps1` must pass before merge

### One-command publish (recommended)

From repo root after bumping versions:

```powershell
.\release\publish-release.ps1 -ReleaseNotes "• bullet points for users"
```

Then open a PR (or commit on `main` if allowed):

```powershell
git checkout -b release/vX.Y.Z-manifest
git add release/version.json
git commit -m "Pin vX.Y.Z manifest SHA256 from live GitHub release asset."
git push -u origin HEAD
gh pr create --base main --title "Pin vX.Y.Z manifest SHA256"
```

## Never do this

- **Do not** set `expectedSha256` from the local APK if the GitHub release was built by CI (`release-apk.yml` on tag push) or another machine — bytes differ (~122 MB local vs ~131 MB CI).
- **Do not** use `/releases/latest/download/` in `apkUrl` — always pin `/releases/download/vX.Y.Z/`.
- **Do not** merge `version.json` before `verify-manifest` passes against the live URL.

## CI tag workflow

Pushing tag `v*` runs `.github/workflows/release-apk.yml`, which builds and uploads `sway_meditation.apk`. That does **not** update `version.json`. After the workflow finishes, run `publish-release.ps1 -SkipBuild` (release already exists) or manually download + hash + verify.

## Verification commands

```powershell
# Manifest vs live GitHub asset (what users download)
.\release\verify-manifest.ps1

# Self-test download/hash helpers (CI)
bash release/verify-manifest.sh --self-test
```
