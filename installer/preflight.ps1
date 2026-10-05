param([string]$InstallRoot, [string]$StopHelper = (Join-Path (Split-Path $PSScriptRoot -Parent) 'connector/shutdown.mjs'))
$ErrorActionPreference = 'Stop'
$taskData = Join-Path $env:LOCALAPPDATA 'DropRun'
$taskFile = Join-Path $taskData 'config.json'
$taskInstall = (Resolve-Path -LiteralPath $InstallRoot).Path
if (Test-Path -LiteralPath $taskFile) {
  $taskConfig = Get-Content -LiteralPath $taskFile -Raw | ConvertFrom-Json
  $taskOrigin = [Uri]$taskConfig.relay
  if ($taskOrigin.Scheme -ne 'https' -or $taskOrigin.UserInfo -or $taskOrigin.AbsolutePath -ne '/' -or $taskOrigin.Query -or $taskOrigin.Fragment) {
    throw 'Invalid saved Relay origin. Repair setup before upgrading.'
  }
  $taskRelay = Invoke-RestMethod ($taskOrigin.AbsoluteUri + 'health') -TimeoutSec 15
  if ($taskRelay.instanceId -ne $taskConfig.instanceId -or $taskRelay.protocolVersion -ne 2 -or $taskRelay.schemaVersion -ne 11 -or $taskRelay.ready -ne $true) {
    throw 'Relay protocol/schema is incompatible. Keep the current installation and follow the release migration guide.'
  }
  $taskHealth = $null
  try { $taskHealth = Invoke-RestMethod 'http://127.0.0.1:47493' -TimeoutSec 3 } catch {}
  $task = Get-ScheduledTask -TaskName ('DropRun Connector ' + $taskConfig.instanceId) -ErrorAction SilentlyContinue
  if (-not $taskHealth -and $task -and $task.State -eq 'Running') { throw 'The running Connector could not be verified. Keep the current installation and try again.' }
  if ($taskHealth -and ($taskHealth.service -ne 'DropRun Connector' -or $taskHealth.instanceId -ne $taskConfig.instanceId)) { throw 'A different service or DropRun instance is running. Keep the current installation.' }
  if ($taskHealth) {
    if ($taskHealth.activeTask) { throw 'Finish or cancel the active DropRun task before installing.' }
  }
  & (Join-Path $taskInstall 'runtime/node.exe') $StopHelper $taskInstall $taskData
  if ($LASTEXITCODE -ne 0) { throw 'The Connector could not be stopped safely.' }
  if ($task) { Stop-ScheduledTask -TaskName $task.TaskName }
}
$taskBackup = Join-Path (Join-Path $env:LOCALAPPDATA 'DropRun-backups') ([Guid]::NewGuid().ToString())
if ($taskBackup.StartsWith($taskInstall.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) {
  throw 'The backup directory must be outside the application directory.'
}
New-Item -ItemType Directory -Path $taskBackup -ErrorAction Stop | Out-Null
Copy-Item -LiteralPath $taskInstall -Destination (Join-Path $taskBackup 'app') -Recurse -ErrorAction Stop
if (Test-Path -LiteralPath $taskData) {
  Copy-Item -LiteralPath $taskData -Destination (Join-Path $taskBackup 'data') -Recurse -ErrorAction Stop
}
@{ createdAt = [DateTime]::UtcNow.ToString('o'); complete = $true; source = $taskInstall } |
  ConvertTo-Json | Set-Content -LiteralPath (Join-Path $taskBackup 'backup.json') -Encoding UTF8
exit 0
