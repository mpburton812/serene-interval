# Release checklist

Use this for every sideload release. Skipping a step causes in-app update hash mismatches.

## Before you start

- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Update `release/CHANGELOG.md`
- [ ] `gh auth status` succeeds (needed for publish script and reliable downloads)

## Root cause of hash mismatches

Two different machines can produce different APK bytes (local Windows build vs CI Ubuntu build). Pushing tag `v*` runs `.github/workflows/release-apk.yml`, which **uploads its own** `safehaven.apk`. If you pin `expectedSha256` from a local upload and CI runs afterward (or vice versa), users see **APK hash mismatch**.

**Rule:** `expectedSha256` must always come from the **live GitHub download**, after all uploads and CI workflows have finished.

## Recommended release flow (tag + CI)

Use this when you push a version tag (CI owns the release asset):

1. Merge app changes to `main` (or promote `dev` → `test` → `main` as usual).
2. **Push tag** — `git tag vX.Y.Z && git push origin vX.Y.Z`
3. **Wait for CI** — `gh run watch --repo mpburton812/serene-interval` until `release-apk.yml` succeeds for that tag.
4. **Pin manifest from live asset** (no local upload):

```powershell
.\release\publish-release.ps1 -PinManifestOnly
```

5. **Pre-deploy gate (required before merge to `main`)**:

```powershell
.\release\pre-deploy-check.ps1 -WaitForCi
```

6. Open PR with `release/version.json`, merge only after step 5 passes locally **and** CI `verify-release-manifest` passes on the PR.

## Alternate flow (manual release, no tag)

Only when you are **not** pushing a `v*` tag (otherwise CI will overwrite the asset):

```powershell
.\release\publish-release.ps1 -ReleaseNotes "• bullet points for users"
```

Creating a **new** release uploads `safehaven.apk`. If the release already exists, you must pass `-Upload` intentionally — default is to refuse, to avoid clobbering CI bytes.

## Mandatory pre-deploy gate

**Run before every manifest PR merge and before telling users an update is live:**

```powershell
.\release\pre-deploy-check.ps1
```

With a freshly pushed tag, wait for CI first:

```powershell
.\release\pre-deploy-check.ps1 -WaitForCi
```

This script:

- Confirms exactly one `safehaven.apk` on the GitHub release (no duplicate APK assets)
- Downloads the live asset twice and ensures the hash is stable
- Runs `verify-manifest.ps1` (manifest `expectedSha256` vs live bytes)

**Do not merge `release/version.json` to `main` unless `pre-deploy-check.ps1` passes.**

## Publish script reference

| Command | Use when |
|---------|----------|
| `publish-release.ps1 -PinManifestOnly` | Tag pushed; CI uploaded APK; pin hash only |
| `publish-release.ps1` | New GitHub release (no existing tag/release) |
| `publish-release.ps1 -Upload` | Intentionally replace an existing release asset |
| `pre-deploy-check.ps1` | Final gate before manifest PR merge / deploy |
| `verify-manifest.ps1` | Quick manifest vs live check |

## Never do this

- **Do not** set `expectedSha256` from the local APK after CI may have uploaded — bytes differ.
- **Do not** run `publish-release.ps1` (without `-PinManifestOnly`) when release `vX.Y.Z` already exists unless you mean to replace the asset with `-Upload`.
- **Do not** push tag `v*` **after** pinning the manifest from a local upload — CI will replace the asset.
- **Do not** use `/releases/latest/download/` in `apkUrl` — always pin `/releases/download/vX.Y.Z/`.
- **Do not** merge `version.json` before `pre-deploy-check.ps1` passes.

## Manifest PR template

```powershell
git checkout -b release/vX.Y.Z-manifest
git add release/version.json
git commit -m "Pin vX.Y.Z manifest SHA256 from live GitHub release asset."
git push -u origin HEAD
gh pr create --base main --title "Pin vX.Y.Z manifest SHA256"
```

Before merge:

```powershell
.\release\pre-deploy-check.ps1
```

## Verification commands

```powershell
# Full pre-deploy gate (recommended)
.\release\pre-deploy-check.ps1

# Manifest vs live GitHub asset only
.\release\verify-manifest.ps1

# Self-test download/hash helpers (CI)
bash release/verify-manifest.sh --self-test
```
