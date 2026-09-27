param([string]$DataDir = (Join-Path $env:LOCALAPPDATA 'DropRun'))
$ErrorActionPreference = 'Stop'
$taskFile = Join-Path $DataDir 'config.json'
if (-not (Test-Path -LiteralPath $taskFile)) { exit 0 }
$taskConfig = Get-Content -LiteralPath $taskFile -Raw | ConvertFrom-Json
$taskRoot = Split-Path $PSScriptRoot -Parent
try { $taskHealth = Invoke-RestMethod 'http://127.0.0.1:47493' -TimeoutSec 3 } catch { $taskHealth = $null }
if ($taskHealth -and $taskHealth.instanceId -eq $taskConfig.instanceId -and $taskHealth.activeTask) { throw 'Finish or cancel the active task before uninstalling DropRun.' }
$taskName = 'DropRun Connector ' + $taskConfig.instanceId
$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($task) { Stop-ScheduledTask -TaskName $taskName; Unregister-ScheduledTask -TaskName $taskName -Confirm:$false }
if ($taskHealth -and $taskHealth.instanceId -eq $taskConfig.instanceId) {
    & (Join-Path $taskRoot 'runtime/node.exe') (Join-Path $taskRoot 'scripts/setup.mjs') stop --data-dir $DataDir
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop this Connector safely.' }
}
# Keep credentials, history and user cloud resources. Uninstall never deletes projects or cloud data.
