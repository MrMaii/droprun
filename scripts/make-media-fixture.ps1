$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
Add-Type -AssemblyName System.Speech
$speech=New-Object System.Speech.Synthesis.SpeechSynthesizer
$speech.SetOutputToWaveFile((Join-Path $repo '.local/test-speech.wav'))
$speech.Speak('DropRun test reference. The button should use green color. The label should say Send to project.')
$speech.Dispose()
& ffmpeg -nostdin -hide_banner -loglevel error -f lavfi -i 'color=c=0x111914:s=640x360:d=10' -i (Join-Path $repo '.local/test-speech.wav') -vf "drawbox=x=180:y=130:w=280:h=100:color=0xb8ef73:t=fill" -shortest -c:v libx264 -pix_fmt yuv420p -c:a aac -y (Join-Path $repo '.local/test-reference.mp4')
if($LASTEXITCODE -ne 0){throw 'Fixture generation failed'}
