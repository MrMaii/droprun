#ifndef PackageDir
  #error PackageDir is required
#endif
#ifndef AppVersion
  #define AppVersion "0.5.11"
#endif
[Setup]
AppId={{0F72564E-B95C-4B37-BE35-462612996874}
AppName=DropRun
AppVersion={#AppVersion}
AppPublisher=DropRun contributors
AppPublisherURL=https://github.com/MrMaii/droprun
DefaultDirName={localappdata}\Programs\DropRun
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#PackageDir}\..
OutputBaseFilename=DropRun-{#AppVersion}-windows-x64-setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\runtime\node.exe
CloseApplications=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "{#PackageDir}\installer\preflight.ps1"; Flags: dontcopy
Source: "{#PackageDir}\connector\shutdown.mjs"; Flags: dontcopy
Source: "{#PackageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{userprograms}\DropRun"; Filename: "powershell.exe"; Parameters: "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File ""{app}\installer\launch.ps1"""; WorkingDir: "{app}"

[Run]
Filename: "powershell.exe"; Parameters: "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File ""{app}\installer\launch.ps1"""; Description: "Connect your own Relay"; Flags: postinstall nowait skipifsilent runhidden

[Code]
function InitializeUninstall(): Boolean;
var Code: Integer;
begin
  Result := Exec('powershell.exe', '-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File "' + ExpandConstant('{app}\installer\uninstall.ps1') + '"', '', SW_HIDE, ewWaitUntilTerminated, Code) and (Code = 0);
  if not Result then
    MsgBox('DropRun could not stop safely. Finish the active task before uninstalling. Your installation has not changed.', mbError, MB_OK);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var Code: Integer;
begin
  Result := '';
  if FileExists(ExpandConstant('{app}\runtime\node.exe')) then begin
    ExtractTemporaryFile('preflight.ps1');
    ExtractTemporaryFile('shutdown.mjs');
    if not Exec('powershell.exe', '-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File "' + ExpandConstant('{tmp}\preflight.ps1') + '" -InstallRoot "' + ExpandConstant('{app}') + '" -StopHelper "' + ExpandConstant('{tmp}\shutdown.mjs') + '"', '', SW_HIDE, ewWaitUntilTerminated, Code) or (Code <> 0) then
      Result := 'Update checks or backup failed. Finish active work, close setup, and verify Relay compatibility and free disk space. Your application files have not been replaced.';
  end;
end;
