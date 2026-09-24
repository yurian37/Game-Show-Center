param(
    [string]$AppVersion = "1.0.0",
    [string]$ReleaseTag = "v1.0"
)

# Script de compilacion y generacion de ejecutable .EXE para Game Show Center Offline
# Desarrollado por Yuyi Studio

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   Game Show Center Offline - Generador de .EXE   " -ForegroundColor Cyan
Write-Host "   Version: $ReleaseTag ($AppVersion)             " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# 1. Compilacion Maven (sin clean para evitar bloqueos)
Write-Host "[1/4] Compilando codigo Java y empaquetando JAR Standalone..." -ForegroundColor Yellow
$mvnResult = & .\mvnw.cmd package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error en la compilacion Maven." -ForegroundColor Red
    exit 1
}

# 2. Localizar artefacto JAR Standalone y preparar staging
Write-Host "[2/4] Preparando artefactos de distribucion..." -ForegroundColor Yellow
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

$stagingDir = "dist-staging"
if (Test-Path $stagingDir) { Remove-Item -Recurse -Force $stagingDir }
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
Copy-Item $jarFile.FullName -Destination "$stagingDir\"

if (-not (Test-Path "release")) {
    New-Item -ItemType Directory -Force -Path "release" | Out-Null
}

if (Test-Path "release\GameShowCenter") {
    $retry = 0
    while ((Test-Path "release\GameShowCenter") -and ($retry -lt 4)) {
        Remove-Item -Recurse -Force "release\GameShowCenter" -ErrorAction SilentlyContinue
        if (Test-Path "release\GameShowCenter") { Start-Sleep -Milliseconds 600 }
        $retry++
    }
}

# 3. Empaquetar con jpackage directamente a release/
Write-Host "[3/4] Creando ejecutable nativo (.EXE) y runtime integrado con jpackage..." -ForegroundColor Yellow
$jpackagePath = "C:\Program Files\Java\jdk-22\bin\jpackage.exe"
if (-not (Test-Path $jpackagePath)) {
    $jpackageCmd = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($jpackageCmd) {
        $jpackagePath = $jpackageCmd.Source
    } else {
        Write-Host "No se encontro jpackage.exe. Por favor verifica que el JDK este instalado." -ForegroundColor Red
        exit 1
    }
}

& $jpackagePath --type app-image `
    --name "GameShowCenter" `
    --input $stagingDir `
    --main-jar $mainJarName `
    --main-class "com.gameshowcenter.offline.Launcher" `
    --dest "release" `
    --vendor "Yuyi Studio" `
    --app-version $AppVersion `
    --description "Game Show Center Offline Executable"

if ($LASTEXITCODE -ne 0) {
    Write-Host "Error al ejecutar jpackage." -ForegroundColor Red
    exit 1
}

# 4. Incluir assets, juegos y configuracion en la distribucion final
Write-Host "[4/4] Copiando recursos, juegos y comprimiendo paquete..." -ForegroundColor Yellow
Copy-Item -Recurse -Force "assets" "release\GameShowCenter\assets"
Copy-Item -Recurse -Force "games" "release\GameShowCenter\games"
if (Test-Path "custom_themes.json") {
    Copy-Item -Force "custom_themes.json" "release\GameShowCenter\"
}
if (Test-Path "scripts") {
    Copy-Item -Recurse -Force "scripts" "release\GameShowCenter\scripts" -Exclude "__pycache__*"
}

# Limpieza de staging
if (Test-Path $stagingDir) { Remove-Item -Recurse -Force $stagingDir }

# Comprimir archivo ZIP
$zipPath = "release\GameShowCenter-Offline-$ReleaseTag.zip"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path "release\GameShowCenter\*" -DestinationPath $zipPath -Force

Write-Host "==================================================" -ForegroundColor Green
Write-Host " [EXITO] Ejecutable generado correctamente en:" -ForegroundColor Green
Write-Host "  -> template-offline\release\GameShowCenter\GameShowCenter.exe" -ForegroundColor White
Write-Host "  -> template-offline\$zipPath" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor Green
