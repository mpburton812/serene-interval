# Mandatory gate before merging a manifest PR or promoting in-app updates to production.
# Downloads the live GitHub release asset and confirms it matches release/version.json.
param(
    [string]$Manifest = "release/version.json",
    [string]$Tag = "",
    [string]$Repo = "mpburton812/serene-interval",
    [switch]$WaitForCi,
    [int]$CiWaitMinutes = 15,
    [int]$StabilityChecks = 2,
    [int]$StabilityDelaySeconds = 10
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$verifyScript = Join-Path $PSScriptRoot "verify-manifest.ps1"
$downloadScript = Join-Path $PSScriptRoot "download-release-apk.ps1"

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Get-ManifestInfo {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Manifest not found: $Path"
    }

    $json = Get-Content -Raw -Path $Path | ConvertFrom-Json
    if (-not $json.versionName) { throw "versionName is required in $Path" }
    if (-not $json.apkUrl) { throw "apkUrl is required in $Path" }

    $expectedSha256 = $null
    if ($json.expectedSha256) {
        $expectedSha256 = ([string]$json.expectedSha256 -replace '\s', '').ToLowerInvariant()
    }

    return [pscustomobject]@{
        VersionName    = [string]$json.versionName
        ApkUrl         = [string]$json.apkUrl
        ExpectedSha256 = $expectedSha256
        Tag            = if ($Tag) { $Tag } else { "v$($json.versionName)" }
    }
}

function Wait-ReleaseApkWorkflow {
    param(
        [string]$WorkflowRepo,
        [string]$ReleaseTag,
        [int]$TimeoutMinutes
    )

    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)
    Write-Host "Waiting for release-apk workflow on tag $ReleaseTag (up to $TimeoutMinutes min) ..."

    while ((Get-Date) -lt $deadline) {
        $runs = gh run list --repo $WorkflowRepo --workflow release-apk.yml --limit 20 --json databaseId,headBranch,status,conclusion,createdAt | ConvertFrom-Json
        $match = $runs | Where-Object { $_.headBranch -eq $ReleaseTag } | Select-Object -First 1

        if (-not $match) {
            Write-Host "No release-apk run for $ReleaseTag yet; retrying in 15s ..."
            Start-Sleep -Seconds 15
            continue
        }

        if ($match.status -eq "in_progress" -or $match.status -eq "queued" -or $match.status -eq "waiting") {
            Write-Host "release-apk run $($match.databaseId) status=$($match.status); waiting ..."
            Start-Sleep -Seconds 15
            continue
        }

        if ($match.conclusion -eq "success") {
            Write-Host "release-apk workflow succeeded for $ReleaseTag."
            return
        }

        throw "release-apk workflow for $ReleaseTag finished with conclusion=$($match.conclusion). Fix CI before deploying."
    }

    throw "Timed out waiting for release-apk workflow on $ReleaseTag."
}

function Test-ReleaseAssets {
    param(
        [string]$WorkflowRepo,
        [string]$ReleaseTag
    )

    $release = gh release view $ReleaseTag --repo $WorkflowRepo --json assets | ConvertFrom-Json
    $apkAssets = @($release.assets | Where-Object { $_.name -eq "sway_meditation.apk" })

    if ($apkAssets.Count -eq 0) {
        throw "Release $ReleaseTag has no sway_meditation.apk asset."
    }
    if ($apkAssets.Count -gt 1) {
        throw "Release $ReleaseTag has $($apkAssets.Count) sway_meditation.apk assets. Remove duplicates before deploy."
    }

    $otherApks = @($release.assets | Where-Object { $_.name -like "*.apk" -and $_.name -ne "sway_meditation.apk" })
    if ($otherApks.Count -gt 0) {
        $names = ($otherApks | ForEach-Object { $_.name }) -join ", "
        throw "Release $ReleaseTag has extra APK assets ($names). Users may download the wrong file."
    }

    $asset = $apkAssets[0]
    Write-Host "Release asset: $($asset.name) ($($asset.size) bytes)"
    if ($asset.digest -and $asset.digest -like "sha256:*") {
        $digest = $asset.digest.Substring(7).ToLowerInvariant()
        Write-Host "GitHub asset digest: $digest"
    }
}

function Test-LiveHashStability {
    param(
        [string]$ApkUrl,
        [string]$ReleaseTag,
        [string]$ExpectedSha256,
        [int]$Checks,
        [int]$DelaySeconds
    )

    if ($Checks -lt 1 -or -not $ExpectedSha256) { return }

    $hashes = [System.Collections.Generic.List[string]]::new()
    for ($i = 1; $i -le $Checks; $i++) {
        $tmpApk = Join-Path ([System.IO.Path]::GetTempPath()) ("predeploy-apk-$i-" + [guid]::NewGuid().ToString() + ".apk")
        try {
            Write-Host "Stability check $i/$Checks ..."
            & $downloadScript -ApkUrl $ApkUrl -OutFile $tmpApk -Tag $ReleaseTag
            $hash = (Get-FileHash -Algorithm SHA256 -Path $tmpApk).Hash.ToLowerInvariant()
            $hashes.Add($hash) | Out-Null
            Write-Host "  live SHA256: $hash"
        }
        finally {
            Remove-Item -Force -ErrorAction SilentlyContinue $tmpApk
        }

        if ($i -lt $Checks -and $DelaySeconds -gt 0) {
            Start-Sleep -Seconds $DelaySeconds
        }
    }

    $unique = $hashes | Select-Object -Unique
    if ($unique.Count -ne 1) {
        throw "Live release asset hash changed between downloads ($($hashes -join ' -> ')). Wait for CI/upload to finish, then re-run publish-release.ps1 -PinManifestOnly."
    }
}

Push-Location $repoRoot
try {
    Require-Command gh

    $manifestPath = if ([System.IO.Path]::IsPathRooted($Manifest)) { $Manifest } else { Join-Path $repoRoot $Manifest }
    $info = Get-ManifestInfo -Path $manifestPath

    Write-Host "Pre-deploy check for $($info.Tag) ..."

    if ($WaitForCi) {
        Wait-ReleaseApkWorkflow -WorkflowRepo $Repo -ReleaseTag $info.Tag -TimeoutMinutes $CiWaitMinutes
        Start-Sleep -Seconds 5
    }

    Test-ReleaseAssets -WorkflowRepo $Repo -ReleaseTag $info.Tag
    Test-LiveHashStability -ApkUrl $info.ApkUrl -ReleaseTag $info.Tag -ExpectedSha256 $info.ExpectedSha256 -Checks $StabilityChecks -DelaySeconds $StabilityDelaySeconds

    Write-Host "Running verify-manifest.ps1 ..."
    & $verifyScript -Manifest $manifestPath

    if (-not $info.ExpectedSha256) {
        Write-Warning "expectedSha256 omitted from manifest; live APK hash not verified."
    }

    Write-Host "Pre-deploy check passed for $($info.Tag)."
}
finally {
    Pop-Location
}
