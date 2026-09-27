$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskNode = Join-Path $taskRoot 'runtime/node.exe'
if (-not (Test-Path -LiteralPath $taskNode)) { $taskNode = (Get-Command node -ErrorAction Stop).Source }
Start-Process -FilePath $taskNode -ArgumentList 'scripts/setup.mjs open' -WorkingDirectory $taskRoot -WindowStyle Hidden
