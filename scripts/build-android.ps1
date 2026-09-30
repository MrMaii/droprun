param([string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if ($OutputDirectory) { $OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory, (Get-Location).Path) }
if (-not $env:ANDROID_HOME -and (Test-Path -LiteralPath (Join-Path $repo '.local/android-sdk'))) { $env:ANDROID_HOME = Join-Path $repo '.local/android-sdk' }
foreach ($key in @('DROPRUN_KEYSTORE','DROPRUN_KEYSTORE_PASSWORD','DROPRUN_KEY_ALIAS','DROPRUN_KEY_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($key)) { throw "Set $key before creating a signed package." }
}
$sourceCommit = & git -C $repo rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'A Git source commit is required.' }
$changes = & git -C $repo status --porcelain
if ($LASTEXITCODE -ne 0 -or $changes) { throw 'Commit or preserve working changes before packaging; source must match a clean Git commit.' }
if ($OutputDirectory -and (Test-Path -LiteralPath $OutputDirectory)) { throw 'Output directory already exists; choose a new directory.' }
& (Join-Path $repo 'android/gradlew.bat') -p (Join-Path $repo 'android') assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
$release = Join-Path $repo 'android/app/build/outputs/apk/release'
$metadata = Get-Content -LiteralPath (Join-Path $release 'output-metadata.json') -Raw | ConvertFrom-Json
if ($metadata.applicationId -ne 'app.droprun.mobile' -or $metadata.variantName -ne 'release' -or @($metadata.elements).Count -ne 1) { throw 'Expected one public release APK.' }
$apk = $metadata.elements[0]
if ($apk.outputFile -ne 'app-release.apk' -or $apk.versionName -notmatch '^\d+\.\d+\.\d+(-[A-Za-z0-9.-]+)?$') { throw 'Unexpected APK filename or version.' }
$name = "DropRun-$($apk.versionName)-android"
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $repo ".local/releases/$name" }
if (Test-Path -LiteralPath $OutputDirectory) { throw 'Output directory already exists; choose a new directory.' }
$signer = Join-Path $env:ANDROID_HOME 'build-tools/35.0.0/apksigner.bat'
$certificate = & $signer verify --print-certs (Join-Path $release $apk.outputFile)
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
$fingerprint = ($certificate | Select-String '^Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})$').Matches.Groups[1].Value
if (-not $fingerprint) { throw 'Signing certificate fingerprint was not reported.' }
$changes = & git -C $repo status --porcelain
if ($LASTEXITCODE -ne 0 -or $changes -or (& git -C $repo rev-parse HEAD) -ne $sourceCommit) { throw 'Source changed during the build; package not created.' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
Copy-Item -LiteralPath (Join-Path $release $apk.outputFile) -Destination (Join-Path $OutputDirectory "$name.apk")
foreach ($file in @('LICENSE','NOTICE','THIRD_PARTY_NOTICES.md')) { Copy-Item -LiteralPath (Join-Path $repo $file) -Destination $OutputDirectory }
New-Item -ItemType Directory -Path (Join-Path $OutputDirectory 'licenses') | Out-Null
Copy-Item -LiteralPath (Join-Path $repo 'licenses/android') -Destination (Join-Path $OutputDirectory 'licenses/android') -Recurse
& git -C $repo -c core.autocrlf=false -c core.eol=lf archive --format=zip "--output=$(Join-Path $OutputDirectory "$name-source.zip")" $sourceCommit -- android
if ($LASTEXITCODE -ne 0) { throw 'Android source archive failed; do not distribute this incomplete directory.' }
$files = @(Get-ChildItem -LiteralPath $OutputDirectory -File -Recurse | ForEach-Object {
    [ordered]@{path=[IO.Path]::GetRelativePath((Resolve-Path $OutputDirectory).Path,$_.FullName).Replace('\','/'); bytes=$_.Length; sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
})
[ordered]@{version=$apk.versionName; versionCode=$apk.versionCode; applicationId=$metadata.applicationId; sourceCommit=$sourceCommit; certificateSha256=$fingerprint; files=$files} |
    ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $OutputDirectory 'BUILD-MANIFEST.json') -Encoding utf8
@($files | ForEach-Object { "$($_.sha256)  $($_.path)" }) + "$((Get-FileHash -LiteralPath (Join-Path $OutputDirectory 'BUILD-MANIFEST.json') -Algorithm SHA256).Hash.ToLowerInvariant())  BUILD-MANIFEST.json" |
    Set-Content -LiteralPath (Join-Path $OutputDirectory 'SHA256SUMS') -Encoding utf8
Write-Output "Signed Android package: $OutputDirectory"
