# Verifies a built APK versionCode/versionName match release/version.json.
param(
    [string]$Apk = "app/build/outputs/apk/debug/app-debug.apk",
    [string]$ManifestPath = "release/version.json"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$apkPath = if ([System.IO.Path]::IsPathRooted($Apk)) { $Apk } else { Join-Path $repoRoot $Apk }
$manifestFile = if ([System.IO.Path]::IsPathRooted($ManifestPath)) { $ManifestPath } else { Join-Path $repoRoot $ManifestPath }

if (-not (Test-Path $apkPath)) {
    throw "APK not found: $apkPath"
}
if (-not (Test-Path $manifestFile)) {
    throw "Manifest not found: $manifestFile"
}

$manifestJson = Get-Content -Raw -Path $manifestFile | ConvertFrom-Json
$expectedCode = [int]$manifestJson.versionCode
$expectedName = [string]$manifestJson.versionName

$aapt = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending |
    ForEach-Object { Join-Path $_.FullName "aapt.exe" } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1

if (-not $aapt) {
    $aaptCmd = Get-Command aapt -ErrorAction SilentlyContinue
    if ($aaptCmd) { $aapt = $aaptCmd.Source }
}

if (-not $aapt) {
    throw "aapt not found. Install Android SDK build-tools or set ANDROID_HOME."
}

$badging = & $aapt dump badging $apkPath 2>&1 | Out-String
$codeMatch = [regex]::Match($badging, "versionCode='(\d+)'")
$nameMatch = [regex]::Match($badging, "versionName='([^']+)'")
if (-not $codeMatch.Success) {
    throw "Could not read versionCode from APK: $apkPath"
}
if (-not $nameMatch.Success) {
    throw "Could not read versionName from APK: $apkPath"
}

$actualCode = [int]$codeMatch.Groups[1].Value
$actualName = $nameMatch.Groups[1].Value

Write-Host "APK versionCode=$actualCode versionName=$actualName"
Write-Host "Manifest versionCode=$expectedCode versionName=$expectedName"

if ($actualCode -ne $expectedCode -or $actualName -ne $expectedName) {
    throw @"
APK metadata mismatch (users will loop on in-app update).
  APK:      versionCode=$actualCode versionName=$actualName
  manifest: versionCode=$expectedCode versionName=$expectedName
Rebuild/upload the correct APK before merging release/version.json.
"@
}

Write-Host "APK metadata matches manifest."
