# Validates release/version.json and confirms apkUrl serves the expected SHA256.
param(
    [string]$Manifest = "release/version.json",
    [string]$LocalApk = ""
)

$ErrorActionPreference = "Stop"

function Test-ManifestJson {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Manifest not found: $Path"
    }

    $json = Get-Content -Raw -Path $Path | ConvertFrom-Json

    if (-not $json.versionCode) { throw "versionCode is required in $Path" }
    if (-not $json.versionName) { throw "versionName is required in $Path" }
    if (-not $json.apkUrl) { throw "apkUrl is required in $Path" }
    if (-not $json.expectedSha256) { throw "expectedSha256 is required in $Path" }

    $script:ExpectedSha256 = ($json.expectedSha256 -replace '\s', '').ToLowerInvariant()
    if ($script:ExpectedSha256 -notmatch '^[0-9a-f]{64}$') {
        throw "expectedSha256 must be 64 hex characters in $Path"
    }

    $script:ApkUrl = [string]$json.apkUrl
    $script:VersionName = [string]$json.versionName

    if ($script:ApkUrl -like "*/releases/latest/download/*") {
        throw "apkUrl must not use /releases/latest/download/ (pin to /releases/download/vX.Y.Z/)"
    }

    $expectedTag = "v$($script:VersionName)"
    if ($script:ApkUrl -notlike "*/releases/download/$expectedTag/*") {
        throw "apkUrl must include /releases/download/$expectedTag/ for versionName $($script:VersionName)"
    }

    if ($script:ApkUrl -notlike "*sway_meditation.apk") {
        throw "apkUrl must end with sway_meditation.apk"
    }
}

function Test-ApkHash {
    param([string]$ApkPath)

    $actual = (Get-FileHash -Algorithm SHA256 -Path $ApkPath).Hash.ToLowerInvariant()
    if ($actual -ne $script:ExpectedSha256) {
        throw "APK hash mismatch.`n  expected: $($script:ExpectedSha256)`n  actual:   $actual"
    }
    Write-Host "APK SHA256 verified: $actual"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    $manifestPath = if ([System.IO.Path]::IsPathRooted($Manifest)) { $Manifest } else { Join-Path $repoRoot $Manifest }
    Test-ManifestJson -Path $manifestPath

    if ($LocalApk) {
        if (-not (Test-Path $LocalApk)) { throw "Local APK not found: $LocalApk" }
        Test-ApkHash -ApkPath $LocalApk
        exit 0
    }

    $tmpApk = Join-Path ([System.IO.Path]::GetTempPath()) ("sway-apk-" + [guid]::NewGuid().ToString() + ".apk")
    try {
        Write-Host "Downloading $($script:ApkUrl) ..."
        $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
        if ($curl) {
            & $curl.Source -fsSL --retry 3 --retry-delay 2 -o $tmpApk $script:ApkUrl
        } else {
            Invoke-WebRequest -Uri $script:ApkUrl -OutFile $tmpApk -UseBasicParsing
        }
        Test-ApkHash -ApkPath $tmpApk
    }
    catch {
        throw "Failed to download or verify apkUrl. Publish the GitHub release before updating version.json.`n$($_.Exception.Message)"
    }
    finally {
        Remove-Item -Force -ErrorAction SilentlyContinue $tmpApk
    }
}
finally {
    Pop-Location
}
