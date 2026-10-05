param([string]$DataDir = '', [string]$InstallRoot = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
$taskRepo = $InstallRoot
if (-not $DataDir) {
    $taskLegacy = Join-Path $taskRepo '.local'
    $DataDir = if (Test-Path -LiteralPath (Join-Path $taskLegacy 'config.json')) { $taskLegacy } else { Join-Path $env:LOCALAPPDATA 'DropRun' }
}
$env:DROPRUN_DATA_DIR = $DataDir
$taskNode = Join-Path $taskRepo 'runtime/node.exe'
if (-not (Test-Path -LiteralPath $taskNode)) { $taskNode = Join-Path $env:ProgramFiles 'nodejs/node.exe' }
if (-not (Test-Path -LiteralPath $taskNode)) { $taskNode = (Get-Command node -ErrorAction Stop).Source }
$taskLogs = Join-Path $DataDir 'service'
New-Item -ItemType Directory -Force $taskLogs | Out-Null
while ($true) {
    try {
        $taskHealth = Invoke-RestMethod 'http://127.0.0.1:47493' -TimeoutSec 3
        if ($taskHealth.service -eq 'DropRun Connector') { exit 0 }
    } catch {}
    $taskWorker = Start-Process -FilePath $taskNode -ArgumentList 'connector/main.mjs' -WorkingDirectory $taskRepo -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskLogs 'connector.stdout.log') -RedirectStandardError (Join-Path $taskLogs 'connector.stderr.log')
    # Retain the handle so Windows PowerShell can read ExitCode after the child exits.
    $taskWorker.Handle | Out-Null
    $taskWorker.WaitForExit()
    if ($taskWorker.ExitCode -eq 0) { exit 0 }
    Start-Sleep -Seconds 10
}
