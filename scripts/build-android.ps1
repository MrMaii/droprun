$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if (-not $env:ANDROID_HOME -and (Test-Path -LiteralPath (Join-Path $repo '.local/android-sdk'))) { $env:ANDROID_HOME = Join-Path $repo '.local/android-sdk' }
if (-not $env:DROPRUN_KEYSTORE) { throw 'Set the four DROPRUN_KEYSTORE/KEY signing environment variables before creating a public release.' }
& (Join-Path $repo 'android/gradlew.bat') -p (Join-Path $repo 'android') assembleRelease --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
New-Item -ItemType Directory -Force (Join-Path $repo '.local/releases') | Out-Null
Copy-Item -LiteralPath (Join-Path $repo 'android/app/build/outputs/apk/release/app-release.apk') -Destination (Join-Path $repo '.local/releases/DropRun-0.5.0-android.apk')
