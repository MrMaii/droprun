param([string]$InstallRoot)
$ErrorActionPreference = 'Stop'
try { $taskHealth = Invoke-RestMethod 'http://127.0.0.1:47493' -TimeoutSec 3 } catch { exit 0 }
if ($taskHealth.activeTask) { Write-Error 'Finish or cancel the active DropRun task before installing.'; exit 1 }
$taskData = Join-Path $env:LOCALAPPDATA 'DropRun'
$taskFile = Join-Path $taskData 'config.json'
if (-not (Test-Path -LiteralPath $taskFile)) { exit 0 }
$taskConfig = Get-Content -LiteralPath $taskFile -Raw | ConvertFrom-Json
if ($taskHealth.instanceId -ne $taskConfig.instanceId) { exit 0 }
$task = Get-ScheduledTask -TaskName ('DropRun Connector ' + $taskConfig.instanceId) -ErrorAction SilentlyContinue
if ($task) { Stop-ScheduledTask -TaskName $task.TaskName }
& (Join-Path $InstallRoot 'runtime/node.exe') (Join-Path $InstallRoot 'scripts/setup.mjs') stop --data-dir $taskData
if ($LASTEXITCODE -ne 0) { exit 1 }
