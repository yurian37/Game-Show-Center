; Script de Inno Setup para Game Show Center - Offline Executable
; Desarrollado por Yuyi Studio

#define MyAppName "Game Show Center Offline"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Yuyi Studio"
#define MyAppURL "https://gameshowcenter.com"
#define MyAppExeName "GameShowCenter.exe"

[Setup]
AppId={{8F634A92-4C1B-4E83-912A-184B425409AC}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\Game Show Center Offline
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=Output
OutputBaseFilename=GameShowCenter-Offline-Setup
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest

[Languages]
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Archivo ejecutable o JAR compilado
Source: "target\template-offline-1.0.0-standalone.jar"; DestDir: "{app}"; Flags: ignoreversion
; Carpeta de minijuegos y assets
Source: "games\*"; DestDir: "{app}\games"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "assets\*"; DestDir: "{app}\assets"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\template-offline-1.0.0-standalone.jar"""; WorkingDir: "{app}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "javaw.exe"; Parameters: "-jar ""{app}\template-offline-1.0.0-standalone.jar"""; WorkingDir: "{app}"; Tasks: desktopicon

[Run]
Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: shellexec postinstall skipifsilent; Filename: "javaw.exe"; Parameters: "-jar ""{app}\template-offline-1.0.0-standalone.jar"""
