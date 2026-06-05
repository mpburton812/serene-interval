# Publishes a sideload APK and writes release/version.json from the live GitHub asset hash.
# NEVER hash the local APK for expectedSha256 — always download back after upload.
param(
    [string]$VersionName = "",
    [int]$VersionCode = 0,
    [string]$ReleaseNotes = "",
    [string]$Tag = "",
    [switch]$SkipBuild,
    [switch]$SkipVerify,
    [switch]$Commit,
    [switch]$Push
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$downloadScript = Join-Path $PSScriptRoot "download-release-apk.ps1"
$verifyScript = Join-Path $PSScriptRoot "verify-manifest.ps1"
$manifestPath = Join-Path $repoRoot "release/version.json"
$localApk = Join-Path $repoRoot "app/build/outputs/apk/debug/app-debug.apk"
$releaseApkName = "sway_meditation.apk"

function Read-GradleVersion {
    param([string]$BuildGradlePath)
    $content = Get-Content -Raw -Path $BuildGradlePath
    if ($content -match 'versionCode\s*=\s*(\d+)') { $script:GradleVersionCode = [int]$Matches[1] }
    if ($content -match 'versionName\s*=\s*"([^"]+)"') { $script:GradleVersionName = $Matches[1] }
}

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

Push-Location $repoRoot
try {
    Require-Command gh

    $buildGradle = Join-Path $repoRoot "app/build.gradle.kts"
    Read-GradleVersion -BuildGradlePath $buildGradle

    $resolvedVersionName = if ($VersionName) { $VersionName } else { $GradleVersionName }
    $resolvedVersionCode = if ($VersionCode -gt 0) { $VersionCode } else { $GradleVersionCode }
    $resolvedTag = if ($Tag) { $Tag } else { "v$resolvedVersionName" }

    if (-not $resolvedVersionName -or $resolvedVersionCode -le 0) {
        throw "Could not resolve version from app/build.gradle.kts. Pass -VersionName and -VersionCode."
    }

    Write-Host "Publishing $resolvedTag (versionCode $resolvedVersionCode) ..."

    if (-not $SkipBuild) {
        Write-Host "Building debug APK (sideload-signed) ..."
        & .\gradlew.bat assembleDebug --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed (exit $LASTEXITCODE)" }
    }

    if (-not (Test-Path $localApk)) {
        throw "Local APK not found: $localApk. Run assembleDebug first or omit -SkipBuild."
    }

    $apkUrl = "https://github.com/mpburton812/serene-interval/releases/download/$resolvedTag/$releaseApkName"
    $releaseExists = $false
    try {
        gh release view $resolvedTag --repo mpburton812/serene-interval 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $releaseExists = $true }
    }
    catch { }

    if ($releaseExists) {
        Write-Host "Release $resolvedTag exists; uploading APK asset ..."
        gh release upload $resolvedTag $localApk --repo mpburton812/serene-interval --clobber
    }
    else {
        $notes = if ($ReleaseNotes) { $ReleaseNotes } else { "Release $resolvedTag" }
        gh release create $resolvedTag $localApk --repo mpburton812/serene-interval `
            --title $resolvedTag --notes $notes
    }
    if ($LASTEXITCODE -ne 0) { throw "gh release upload/create failed (exit $LASTEXITCODE)" }

    Write-Host "Waiting for GitHub release asset to propagate ..."
    Start-Sleep -Seconds 5

    $tmpApk = Join-Path ([System.IO.Path]::GetTempPath()) ("publish-apk-" + [guid]::NewGuid().ToString() + ".apk")
    try {
        Write-Host "Downloading live release asset (source of truth for expectedSha256) ..."
        & $downloadScript -ApkUrl $apkUrl -OutFile $tmpApk -Tag $resolvedTag
        $sha256 = (Get-FileHash -Algorithm SHA256 -Path $tmpApk).Hash.ToLowerInvariant()
        $size = (Get-Item $tmpApk).Length
        Write-Host "Live APK SHA256: $sha256 ($size bytes)"
    }
    finally {
        Remove-Item -Force -ErrorAction SilentlyContinue $tmpApk
    }

    $notesValue = $ReleaseNotes
    if (-not $notesValue -and (Test-Path $manifestPath)) {
        $existing = Get-Content -Raw -Path $manifestPath | ConvertFrom-Json
        if ($existing.releaseNotes) { $notesValue = [string]$existing.releaseNotes }
    }
    if (-not $notesValue) { $notesValue = "Release $resolvedTag" }

    $manifest = [ordered]@{
        versionCode     = $resolvedVersionCode
        versionName     = $resolvedVersionName
        apkUrl          = $apkUrl
        expectedSha256  = $sha256
        releaseNotes    = $notesValue
        minVersionCode  = 1
    }

    $json = ($manifest | ConvertTo-Json -Depth 5) + "`n"
    Set-Content -Path $manifestPath -Value $json -Encoding utf8NoBOM -NoNewline
    Write-Host "Wrote $manifestPath"

    if (-not $SkipVerify) {
        Write-Host "Verifying manifest against live APK ..."
        & $verifyScript -Manifest $manifestPath
    }

    if ($Commit) {
        git add release/version.json
        git commit -m "Pin $resolvedTag manifest SHA256 from live GitHub release asset."
        if ($Push) {
            git push
        }
    }
    else {
        Write-Host "Manifest updated. Commit with:"
        Write-Host "  git add release/version.json"
        Write-Host "  git commit -m `"Pin $resolvedTag manifest SHA256 from live GitHub release asset.`""
        Write-Host "  git push"
    }

    Write-Host "Done. expectedSha256=$sha256"
}
finally {
    Pop-Location
}
