# Builds and runs Espresso / Compose instrumented tests on a connected device or emulator.
# Run from repo root in PowerShell: .\tools\run-instrumented-tests.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

if (-not $env:JAVA_HOME) {
    $candidates = @(
        "${env:ProgramFiles}\Android\Android Studio\jbr",
        "${env:LOCALAPPDATA}\Programs\Android Studio\jbr"
    )
    foreach ($path in $candidates) {
        if (Test-Path "$path\bin\java.exe") {
            $env:JAVA_HOME = $path
            $env:Path = "$path\bin;$env:Path"
            Write-Host "Using JAVA_HOME=$path"
            break
        }
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "java not found. Set JAVA_HOME to Android Studio's jbr folder."
}

$adb = "adb"
$sdkAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (Test-Path $sdkAdb) { $adb = $sdkAdb }

Write-Host "Checking devices ..."
& $adb devices
$devices = & $adb devices | Select-String "device$"
if (-not $devices) {
    throw "No emulator/device connected. Start an AVD in Android Studio, then rerun."
}

Write-Host "Cleaning old instrumented test outputs (fixes corrupt TEST-*.xml on Windows) ..."
$paths = @(
    "app\build\outputs\androidTest-results",
    "app\build\reports\androidTests"
)
foreach ($p in $paths) {
    if (Test-Path $p) { Remove-Item -Recurse -Force $p }
}

Write-Host "Building and running connectedDebugAndroidTest ..."
& .\gradlew.bat clean :app:connectedDebugAndroidTest --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Done. Report: app\build\reports\androidTests\connected\debug\index.html"
