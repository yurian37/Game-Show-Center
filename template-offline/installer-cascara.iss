; Script de Inno Setup para la CÁSCARA Standalone Virgen de Game Show Center v1.1
; Desarrollado por YonIA Enterprise
; Incluye ejecutable nativo (.exe) y runtime JRE embebido sin requerir Java ni Python en el sistema del usuario.
; Instalación virgen: sin licencias activas, sin juegos, sin megacores y con solo roulette en assets/games/.

#define MyAppName "Game Show Center (Cascara Offline)"
#define MyAppVersion "1.1"
#define MyAppPublisher "YonIA Enterprise"
#define MyAppURL "https://gameshowcenter.com"
#define MyAppExeName "GameShowCenter.exe"

[Setup]
AppId={{C1984E72-88B1-4DF5-91FA-7164D8D203AF}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\Game Show Center Offline
DefaultGroupName=Game Show Center Offline
DisableProgramGroupPage=yes
OutputDir=release
OutputBaseFilename=GameShowCenter-Cascara-Setup
SetupIconFile=assets\icons\app_icon.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
; Firma digital Authenticode (activada cuando se pasa el parametro /S o SignTool en Inno Setup)
; SignTool=signtool sign /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 /f $q%GSC_SIGNING_CERT_PATH%$q /p $q%GSC_SIGNING_CERT_PASSWORD%$q $f

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Dirs]
Name: "{app}\games"; Permissions: users-full
Name: "{app}\megacores"; Permissions: users-full

[InstallDelete]
; Limpiar cualquier rastro previo para garantizar una instalacion 100% virgen
Type: files; Name: "{app}\license.lic"
Type: files; Name: "{app}\last_match_setup.json"
Type: files; Name: "{app}\preferences.json"
Type: filesandordirs; Name: "{app}\games\*"
Type: filesandordirs; Name: "{app}\megacores\*"
Type: filesandordirs; Name: "{app}\assets\games\flipcoin"
Type: filesandordirs; Name: "{app}\assets\games\guessthecharacter"
Type: filesandordirs; Name: "{app}\assets\games\hangman"
Type: filesandordirs; Name: "{app}\assets\games\tictactoe"
Type: filesandordirs; Name: "{app}\assets\games\zeromargin"

[Files]
; Todos los archivos de la distribución Cáscara (ejecutable nativo, runtime Java integrado, app, assets virgen, scripts y temas)
Source: "release\GameShowCenter-Cascara\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"
Name: "{group}\Carpeta de MegaCores"; Filename: "{app}\megacores"; WorkingDir: "{app}"
Name: "{group}\Desinstalar Game Show Center"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent; Filename: "{app}\{#MyAppExeName}"

[Code]
procedure CurStepChanged(CurStep: TSetupStep);
var
  HomeDir: String;
begin
  if CurStep = ssInstall then
  begin
    // Desactivar y limpiar cualquier licencia previa en el perfil de usuario
    HomeDir := GetEnv('USERPROFILE');
    if HomeDir <> '' then
    begin
      DeleteFile(HomeDir + '\.gsc_offline\license.lic');
      RemoveDir(HomeDir + '\.gsc_offline');
    end;
  end;
end;
