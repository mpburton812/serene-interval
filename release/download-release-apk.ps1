# Downloads a GitHub release APK using the same transports end users hit.
# Used by verify-manifest.ps1 and publish-release.ps1 so hashes match live bytes.
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkUrl,

    [Parameter(Mandatory = $true)]
    [string]$OutFile,

    [string]$Tag = ""
)

$ErrorActionPreference = "Stop"

function Get-TagFromApkUrl {
    param([string]$Url)
    if ($Url -match '/releases/download/(v[^/]+)/') {
        return $Matches[1]
    }
    return ""
}

function Test-DownloadedApk {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        throw "Download produced no file: $Path"
    }
    $size = (Get-Item $Path).Length
    if ($size -lt 1MB) {
        $snippet = ""
        if ($size -gt 0) {
            $bytes = [System.IO.File]::ReadAllBytes($Path)
            $snippet = [System.Text.Encoding]::UTF8.GetString($bytes[0..([Math]::Min(200, $bytes.Length - 1))])
        }
        throw "Downloaded file is too small ($size bytes). Response may be an HTML error page.`n$snippet"
    }
}

$resolvedTag = if ($Tag) { $Tag } else { Get-TagFromApkUrl -Url $ApkUrl }
$errors = [System.Collections.Generic.List[string]]::new()

# 1) gh release download — same asset GitHub serves; works when gh is authenticated.
if ($resolvedTag -and (Get-Command gh -ErrorAction SilentlyContinue)) {
    try {
        $dlDir = Join-Path ([System.IO.Path]::GetTempPath()) ("gh-apk-" + [guid]::NewGuid().ToString())
        New-Item -ItemType Directory -Path $dlDir -Force | Out-Null
        try {
            & gh release download $resolvedTag --repo mpburton812/serene-interval --pattern "safehaven.apk" --dir $dlDir --clobber
            if ($LASTEXITCODE -eq 0) {
                $downloaded = Join-Path $dlDir "safehaven.apk"
                if (Test-Path $downloaded) {
                    Copy-Item -Force $downloaded $OutFile
                    Test-DownloadedApk -Path $OutFile
                    Write-Host "Downloaded via gh release download ($resolvedTag)"
                    return
                }
            }
            $errors.Add("gh release download failed (exit $LASTEXITCODE)")
        }
        finally {
            Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $dlDir
        }
    }
    catch {
        $errors.Add("gh release download: $($_.Exception.Message)")
    }
}

# 2) curl.exe — follow redirects; GitHub release URLs redirect to object storage.
$curl = Get-Command curl.exe -ErrorAction SilentlyContinue
if ($curl) {
    try {
        & $curl.Source -fsSL --retry 3 --retry-delay 2 `
            -H "Accept: application/octet-stream" `
            -H "User-Agent: serene-interval-manifest-verify" `
            -o $OutFile $ApkUrl
        if ($LASTEXITCODE -eq 0) {
            Test-DownloadedApk -Path $OutFile
            Write-Host "Downloaded via curl"
            return
        }
        $errors.Add("curl failed (exit $LASTEXITCODE)")
    }
    catch {
        $errors.Add("curl: $($_.Exception.Message)")
    }
}

# 3) Invoke-WebRequest — fallback when curl is missing or returns 404 without -L.
try {
    $headers = @{
        Accept         = "application/octet-stream"
        "User-Agent"   = "serene-interval-manifest-verify"
    }
    Invoke-WebRequest -Uri $ApkUrl -OutFile $OutFile -Headers $headers -UseBasicParsing
    Test-DownloadedApk -Path $OutFile
    Write-Host "Downloaded via Invoke-WebRequest"
    return
}
catch {
    $errors.Add("Invoke-WebRequest: $($_.Exception.Message)")
}

$detail = ($errors | ForEach-Object { "  - $_" }) -join "`n"
throw "All download methods failed for $ApkUrl`n$detail"
