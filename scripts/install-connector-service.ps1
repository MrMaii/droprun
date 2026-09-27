param([string]$DataDir = (Join-Path $env:LOCALAPPDATA 'DropRun'), [string]$InstallRoot = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
$taskScript = Join-Path $InstallRoot 'scripts/connector-service.ps1'
if (-not (Test-Path -LiteralPath (Join-Path $DataDir 'config.json'))) { throw 'Complete DropRun setup before starting the Connector.' }
$taskConfig = Get-Content -LiteralPath (Join-Path $DataDir 'config.json') -Raw | ConvertFrom-Json
try {
    $taskHealth = Invoke-RestMethod 'http://127.0.0.1:47493' -TimeoutSec 3
    if ($taskHealth.service -eq 'DropRun Connector') {
        if ($taskHealth.instanceId -ne $taskConfig.instanceId) { throw 'Another DropRun instance is using this computer. Stop that instance explicitly before starting this one.' }
        Write-Output 'This Connector is already running.'
        exit 0
    }
} catch { if ($_.Exception.Message -like 'Another DropRun*') { throw } }
$taskUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$taskAction = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument ('-NoProfile -NonInteractive -WindowStyle Hidden -File "' + $taskScript + '" -DataDir "' + $DataDir + '" -InstallRoot "' + $InstallRoot + '"')
$taskTrigger = New-ScheduledTaskTrigger -AtLogOn -User $taskUser
$taskSettings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
$taskPrincipal = New-ScheduledTaskPrincipal -UserId $taskUser -LogonType Interactive -RunLevel Limited
$taskName = 'DropRun Connector ' + $taskConfig.instanceId
Register-ScheduledTask -TaskName $taskName -Action $taskAction -Trigger $taskTrigger -Settings $taskSettings -Principal $taskPrincipal -Description 'Connect Android DropRun shares to local Codex projects after sign-in.' -Force | Out-Null
Start-ScheduledTask -TaskName $taskName
Get-ScheduledTask -TaskName $taskName | Select-Object TaskName, State
