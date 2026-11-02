param(
    [string]$AppVersion = "1.1.0",
    [string]$ReleaseTag = "v1.1"
)

# Script de compilacion y generacion del ejecutable (.EXE) e instalador de la CASCARA VIRGEN de Game Show Center
# Desarrollado por YonIA Enterprise - 2026

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   Game Show Center - CASCARA VIRGEN v1.1 (Build) " -ForegroundColor Cyan
Write-Host "   Version: $ReleaseTag ($AppVersion)             " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Compilar codigo Java con Maven y empaquetar Standalone JAR
Write-Host "[1/5] Compilando Java y generando Uber-JAR Standalone..." -ForegroundColor Yellow
& .\mvnw.cmd package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error durante la compilacion de Maven." -ForegroundColor Red
    exit 1
}

# 2. Localizar artefacto JAR Standalone
Write-Host "[2/5] Verificando artefacto JAR Standalone..." -ForegroundColor Yellow
$jarFile = Get-ChildItem "target\*$AppVersion*standalone.jar" | Select-Object -First 1
if (-not $jarFile) {
    $jarFile = Get-ChildItem "target\*standalone.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}
if (-not $jarFile) {
    Write-Host "No se encontro el archivo JAR standalone en target\." -ForegroundColor Red
    exit 1
}
$mainJarName = $jarFile.Name
Write-Host "  -> Utilizando JAR: $mainJarName" -ForegroundColor DarkGray

# Asegurar existencia del icono de la aplicacion
if (-not (Test-Path "assets\icons\app_icon.ico")) {
    Write-Host "  -> Generando icono nativo assets\icons\app_icon.ico..." -ForegroundColor DarkGray
    if (Test-Path "make-icon.ps1") {
        & powershell -ExecutionPolicy Bypass -File .\make-icon.ps1 | Out-Null
    }
}

# 3. Empaquetar ejecutable nativo (.EXE) y JRE integrado con jpackage
Write-Host "[3/5] Creando ejecutable nativo (.EXE) y runtime JRE embebido con jpackage..." -ForegroundColor Yellow

$jpackageCmd = Get-Command jpackage -ErrorAction SilentlyContinue
$jpackagePath = $null
if ($jpackageCmd) {
    $jpackagePath = $jpackageCmd.Source
} else {
    $candidates = @(
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\jpackage.exe",
        "C:\Program Files\Java\jdk-22\bin\jpackage.exe",
        "C:\Program Files\Java\jdk-21\bin\jpackage.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) {
            $jpackagePath = $c
            break
        }
    }
}

if (-not $jpackagePath) {
    Write-Host "No se encontro jpackage.exe. Verifica que el JDK este instalado." -ForegroundColor Red
    exit 1
}
Write-Host "  -> Compilador jpackage: $jpackagePath" -ForegroundColor DarkGray

$stagingDir = "dist-staging"
if (Test-Path $stagingDir) { Remove-Item -Recurse -Force $stagingDir }
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
Copy-Item $jarFile.FullName -Destination $stagingDir

$tempJpackageDir = "release\temp-jpackage"
if (Test-Path $tempJpackageDir) { Remove-Item -Recurse -Force $tempJpackageDir }
New-Item -ItemType Directory -Force -Path $tempJpackageDir | Out-Null

$jpArgs = @(
    "--type", "app-image",
    "--name", "GameShowCenter",
    "--input", $stagingDir,
    "--main-jar", $mainJarName,
    "--main-class", "com.gameshowcenter.offline.Launcher",
    "--dest", $tempJpackageDir,
    "--vendor", "YonIA Enterprise",
    "--app-version", $AppVersion,
    "--description", "Game Show Center Offline Executable"
)

if (Test-Path "assets\icons\app_icon.ico") {
    $jpArgs += @("--icon", "assets\icons\app_icon.ico")
}

& $jpackagePath @jpArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error al ejecutar jpackage." -ForegroundColor Red
    exit 1
}

# 4. Ensamblar la distribucion limpia y 100% VIRGEN de la CASCARA
Write-Host "[4/5] Ensamblando la estructura limpia y VIRGEN de la Cascara..." -ForegroundColor Yellow
$cascaraDir = "release\GameShowCenter-Cascara"
if (Test-Path $cascaraDir) {
    Remove-Item -Recurse -Force $cascaraDir -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Force -Path $cascaraDir | Out-Null

# Copiar el runtime, ejecutable y app de jpackage
Copy-Item -Recurse -Force "$tempJpackageDir\GameShowCenter\*" $cascaraDir
Remove-Item -Recurse -Force $tempJpackageDir -ErrorAction SilentlyContinue
if (Test-Path $stagingDir) { Remove-Item -Recurse -Force $stagingDir }

# Copiar recursos base de la cascara
Copy-Item -Recurse -Force "assets" (Join-Path $cascaraDir "assets")

# REGLA ESTRICTA DE ASSETS:
# El UNICO asset de minijuego que viene integrado en la cáscara es 'roulette' (ruleta de selección).
# Todos los demás assets de juegos (hangman, tictactoe, zeromargin, flipcoin, guessthecharacter)
# se remueven de la cáscara virgen porque se cargan exclusivamente desde los paquetes MegaCore.
$cascaraGamesAssets = Join-Path $cascaraDir "assets\games"
if (Test-Path $cascaraGamesAssets) {
    Get-ChildItem -Path $cascaraGamesAssets -Directory | Where-Object { $_.Name -ne "roulette" } | ForEach-Object {
        Remove-Item -Recurse -Force $_.FullName -ErrorAction SilentlyContinue
    }
}

if (Test-Path "scripts") {
    Copy-Item -Recurse -Force "scripts" (Join-Path $cascaraDir "scripts") -Exclude "__pycache__*"
}
if (Test-Path "custom_themes.json") {
    Copy-Item -Force "custom_themes.json" $cascaraDir
}

# Carpetas games y megacores 100% VIRGENES (SIN juegos ni paquetes MegaCore incluidos)
$gamesFolder = Join-Path $cascaraDir "games"
$megacoresFolder = Join-Path $cascaraDir "megacores"
New-Item -ItemType Directory -Force -Path $gamesFolder | Out-Null
New-Item -ItemType Directory -Force -Path $megacoresFolder | Out-Null

Set-Content -Path (Join-Path $gamesFolder "README_GAMES.txt") -Value "Directorio donde se instalan los minijuegos de Game Show Center al cargar un paquete MegaCore."
Set-Content -Path (Join-Path $megacoresFolder "README_MEGACORES.txt") -Value "Directorio para depositar los paquetes comprimidos de minijuegos (.zip o .megacore) adquiridos."

# SEGURIDAD COMERCIAL Y ESTADO VIRGEN:
# Garantizar que NINGUN archivo de licencia (.lic), generador ni estado previo de partidas esté en la distribución
Get-ChildItem -Path $cascaraDir -Include "*generate-license*", "*.lic", "last_match_setup.json", "preferences.json" -Recurse | Remove-Item -Force -ErrorAction SilentlyContinue

$launcherBat = @"
@echo off
cd /d "%~dp0"
start "" "GameShowCenter.exe"
"@
[System.IO.File]::WriteAllText((Join-Path $cascaraDir "Lanzar-GameShowCenter.bat"), $launcherBat, [System.Text.Encoding]::ASCII)

$readmeContent = @"
======================================================================
  GAME SHOW CENTER OFFLINE - CASCARA BASE VIRGEN v$AppVersion
  Desarrollado por YonIA Enterprise
======================================================================

Esta es la instalacion 100% limpia y virgen de Game Show Center Offline.
Incluye runtime JRE embebido, por lo que NO requiere Java ni Python en el sistema.

INSTRUCCIONES DE USO:
1. EJECUCION:
   - Haz doble clic sobre 'GameShowCenter.exe' o 'Lanzar-GameShowCenter.bat'.
   - Funciona de forma 100% autonoma sin necesidad de instalar Java.

2. ACTIVACION DE LICENCIA COMERCIAL:
   - Al iniciar por primera vez, la aplicacion mostrara la pantalla de activacion.
   - Solicita tu archivo de licencia anual (.lic) al administrador o vendedor autorizado.
   - Carga el archivo en la pantalla para activar la plataforma por 365 dias (vinculada al hardware de este equipo).

3. MINIJUEGOS (MEGACORES):
   - Esta instalacion virgen es modular y no incluye juegos preinstalados.
   - Para cargar actividades, utiliza el boton 'MegaCores' en la aplicacion
     y selecciona el archivo .zip de cualquier MegaCore adquirido (MegaCore 1, 2, 3...).
     El sistema instalara automaticamente las actividades y sus recursos multimedia requeridos.
======================================================================
"@
[System.IO.File]::WriteAllText((Join-Path $cascaraDir "README-CASCARA.txt"), $readmeContent, [System.Text.Encoding]::UTF8)

# Comprimir paquete portable ZIP de la cascara virgen (con ejecutable .exe y JRE embebido)
$zipCascara = "release\GameShowCenter-Cascara-$ReleaseTag.zip"
if (Test-Path $zipCascara) { Remove-Item -Force $zipCascara }
Compress-Archive -Path "$cascaraDir\*" -DestinationPath $zipCascara -Force
$zipLen = (Get-Item $zipCascara).Length
Write-Host "  -> Paquete ZIP portable con .EXE virgen creado: $zipCascara ($([math]::Round($zipLen/1MB, 2)) MB)" -ForegroundColor Green

# 5. Compilar instalador Inno Setup (.exe) si el compilador ISCC esta disponible
Write-Host "[5/5] Compilando Instalador Windows (.EXE) con Inno Setup (ISCC)..." -ForegroundColor Yellow

$isccPath = $null
$isccCmd = Get-Command iscc -ErrorAction SilentlyContinue
if ($isccCmd) {
    $isccPath = $isccCmd.Source
} else {
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\Antigravity IDE\resources\app\node_modules\innosetup\bin\ISCC.exe",
        "C:\Program Files (x86)\Inno Setup 6\ISCC.exe",
        "C:\Program Files\Inno Setup 6\ISCC.exe",
        "C:\Program Files (x86)\Inno Setup 5\ISCC.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) {
            $isccPath = $c
            break
        }
    }
}

$installerExe = "release\GameShowCenter-Cascara-Setup.exe"
if ($isccPath) {
    Write-Host "  -> Compilador Inno Setup detectado en: $isccPath" -ForegroundColor DarkGray
    Write-Host "  -> Compilando instalador virgen autonomo..." -ForegroundColor DarkGray
    & $isccPath "installer-cascara.iss"
    if ($LASTEXITCODE -eq 0 -and (Test-Path $installerExe)) {
        $instLen = (Get-Item $installerExe).Length
        Write-Host "  -> [EXITO] Instalador virgen compilado: $installerExe ($([math]::Round($instLen/1MB, 2)) MB)" -ForegroundColor Green
    } else {
        Write-Host "  -> Advertencia: Fallo al compilar el script Inno Setup." -ForegroundColor Yellow
    }
} else {
    Write-Host "  -> Inno Setup no detectado en PATH. Script 'installer-cascara.iss' listo para compilar manualmente con Inno Setup Compiler." -ForegroundColor DarkGray
}

Write-Host "`n==================================================" -ForegroundColor Green
Write-Host " [COMPILACION VIRGEN COMPLETADA CON EXITO]" -ForegroundColor Green
Write-Host "  1. Ejecutable nativo (.EXE con JRE embebido, virgen):" -ForegroundColor White
Write-Host "     -> $cascaraDir\GameShowCenter.exe" -ForegroundColor White
if (Test-Path $installerExe) {
    Write-Host "  2. Instalador de Windows (.EXE virgen):" -ForegroundColor White
    Write-Host "     -> $installerExe" -ForegroundColor White
}
Write-Host "  3. Paquete portable comprimido (.ZIP virgen):" -ForegroundColor White
Write-Host "     -> $zipCascara" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor Green
