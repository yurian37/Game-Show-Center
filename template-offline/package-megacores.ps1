param(
    [string]$GamesSourceDir = "games",
    [string]$AssetsSourceDir = "assets",
    [string]$OutDir = "release\megacores"
)

# Empaquetador oficial de MegaCores para Game Show Center Offline (v1.1)
# YonIA Enterprise 2026
# Soporte para empaquetado de juegos y sus recursos (assets) asociados.

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

if (-not (Test-Path $GamesSourceDir)) {
    if (Test-Path "template-offline\games") {
        $GamesSourceDir = "template-offline\games"
    } else {
        Write-Host "ERROR: Directorio de juegos no encontrado: $GamesSourceDir" -ForegroundColor Red
        exit 1
    }
}

if (-not (Test-Path $AssetsSourceDir)) {
    if (Test-Path "template-offline\assets") {
        $AssetsSourceDir = "template-offline\assets"
    }
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "   GENERADOR DE PAQUETES MEGACORE (v1.1)          " -ForegroundColor Cyan
Write-Host "   Empaqueta Juegos + Recursos (Assets)           " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Directorio origen de juegos: $GamesSourceDir" -ForegroundColor DarkGray
Write-Host "Directorio origen de assets: $AssetsSourceDir" -ForegroundColor DarkGray

if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
}
if (-not (Test-Path "megacores")) {
    New-Item -ItemType Directory -Force -Path "megacores" | Out-Null
}

$stagingBase = "target\megacore-staging"
if (Test-Path $stagingBase) {
    Remove-Item -Recurse -Force $stagingBase -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Force -Path $stagingBase | Out-Null

function Copy-GameAndAssets {
    param(
        [string]$GameName,
        [string]$StageDir
    )
    
    # 1. Copiar carpeta del juego (código, metadatos, setup, ai_prompt, imágenes locales)
    $src = Join-Path $GamesSourceDir $GameName
    if ((-not (Test-Path $src)) -and ($GameName -eq "Snap Solve")) {
        $src = Join-Path $GamesSourceDir "Snap"
    }
    
    if (Test-Path $src) {
        Write-Host "  -> Minijuego: $GameName" -ForegroundColor DarkGray
        Copy-Item -Recurse -Force $src (Join-Path $StageDir $GameName)
    } else {
        Write-Host "  Advertencia: No se encontro juego $src" -ForegroundColor Red
    }

    # 2. Copiar assets vinculados en assets/games/<directorio>
    if (Test-Path $AssetsSourceDir) {
        $clean = ($GameName -replace '\s+', '').ToLower()
        $underscore = ($GameName -replace '\s+', '_').ToLower()
        
        $assetCandidates = @(
            $clean,
            $underscore,
            $GameName
        )

        # Mapeos especiales y dependencias cruzadas de recursos
        if ($clean -eq "guesscharacter") {
            $assetCandidates += "guessthecharacter"
            $assetCandidates += "zeromargin"  # Utiliza digitos y colon de zeromargin
        }
        if ($clean -eq "hangman") {
            $assetCandidates += "hangman"
        }
        if ($clean -eq "tictactoe") {
            $assetCandidates += "tictactoe"
        }
        if ($clean -eq "zeromargin") {
            $assetCandidates += "zeromargin"
        }
        if ($clean -eq "snapsolve") {
            $assetCandidates += "snap"
        }
        if ($clean -eq "flipcoin") {
            $assetCandidates += "flipcoin"
        }
        if ($clean -eq "roulette") {
            $assetCandidates += "roulette"
            $assetCandidates += "flipcoin"
        }

        $copiedFolders = @()
        foreach ($folder in ($assetCandidates | Select-Object -Unique)) {
            $candidatePath = Join-Path $AssetsSourceDir "games\$folder"
            if (Test-Path $candidatePath) {
                $targetAssetDir = Join-Path $StageDir "assets\games\$folder"
                if (-not (Test-Path $targetAssetDir)) {
                    New-Item -ItemType Directory -Force -Path $targetAssetDir | Out-Null
                }
                Copy-Item -Recurse -Force "$candidatePath\*" $targetAssetDir
                $copiedFolders += $folder
            }
        }
        if ($copiedFolders.Count -gt 0) {
            Write-Host "     + Assets incluidos: ($($copiedFolders -join ', '))" -ForegroundColor DarkCyan
        }
    }
}

# -------------------------------------------------------------------
# MEGACORE 1: TimeLine, Hangman, TicTacToe, Trivia Quiz, Zero Margin
# -------------------------------------------------------------------
Write-Host ""
Write-Host "[1/2] Empaquetando MegaCore 1 (Juegos + Assets)..." -ForegroundColor Yellow
$core1Games = @("TimeLine", "Hangman", "TicTacToe", "Trivia Quiz", "Zero Margin")
$stage1 = Join-Path $stagingBase "MegaCore-1"
New-Item -ItemType Directory -Force -Path $stage1 | Out-Null

foreach ($game in $core1Games) {
    Copy-GameAndAssets -GameName $game -StageDir $stage1
}

$manifest1 = @{
    megacoreNumber = 1
    title = "MegaCore 1"
    description = "Paquete oficial de minijuegos MegaCore 1 para Game Show Center (con assets)"
    version = "1.1.0"
    games = $core1Games
} | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText((Join-Path $stage1 "megacore.json"), $manifest1, [System.Text.Encoding]::UTF8)

$zip1Rel = Join-Path $OutDir "MegaCore-1.zip"
if (Test-Path $zip1Rel) { Remove-Item -Force $zip1Rel }
Compress-Archive -Path "$stage1\*" -DestinationPath $zip1Rel -Force
Copy-Item -Force $zip1Rel "megacores\MegaCore-1.zip"

$len1 = (Get-Item $zip1Rel).Length
Write-Host "  MegaCore 1 generado con exito: $zip1Rel ($len1 bytes)" -ForegroundColor Green

# -------------------------------------------------------------------
# MEGACORE 2: GeoLocation, Guess Character, Rapid Rhythm, Snap Solve, Topic Takedown
# -------------------------------------------------------------------
Write-Host ""
Write-Host "[2/2] Empaquetando MegaCore 2 (Juegos + Assets)..." -ForegroundColor Yellow
$core2Games = @("GeoLocation", "Guess Character", "Rapid Rhythm", "Snap Solve", "Topic Takedown")
$stage2 = Join-Path $stagingBase "MegaCore-2"
New-Item -ItemType Directory -Force -Path $stage2 | Out-Null

foreach ($game in $core2Games) {
    Copy-GameAndAssets -GameName $game -StageDir $stage2
}

$manifest2 = @{
    megacoreNumber = 2
    title = "MegaCore 2"
    description = "Paquete oficial de minijuegos MegaCore 2 para Game Show Center (con assets)"
    version = "1.1.0"
    games = @("GeoLocation", "Guess Character", "Rapid Rhythm", "Snap Solve", "Topic Takedown")
} | ConvertTo-Json -Depth 4
[System.IO.File]::WriteAllText((Join-Path $stage2 "megacore.json"), $manifest2, [System.Text.Encoding]::UTF8)

$zip2Rel = Join-Path $OutDir "MegaCore-2.zip"
if (Test-Path $zip2Rel) { Remove-Item -Force $zip2Rel }
Compress-Archive -Path "$stage2\*" -DestinationPath $zip2Rel -Force
Copy-Item -Force $zip2Rel "megacores\MegaCore-2.zip"

$len2 = (Get-Item $zip2Rel).Length
Write-Host "  MegaCore 2 generado con exito: $zip2Rel ($len2 bytes)" -ForegroundColor Green

# Sincronizar tambien a la carpeta portable de la cascara si existe
$cascaraMegaDir = "release\GameShowCenter-Cascara\megacores"
if (Test-Path $cascaraMegaDir) {
    Copy-Item -Force $zip1Rel (Join-Path $cascaraMegaDir "MegaCore-1.zip")
    Copy-Item -Force $zip2Rel (Join-Path $cascaraMegaDir "MegaCore-2.zip")
    Write-Host "  -> Paquetes sincronizados a: $cascaraMegaDir" -ForegroundColor DarkGray
}

# Limpieza
Remove-Item -Recurse -Force $stagingBase -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host " [EXITO] Paquetes MegaCore generados y listos (v1.1):" -ForegroundColor Green
Write-Host "  -> $zip1Rel" -ForegroundColor White
Write-Host "  -> $zip2Rel" -ForegroundColor White
Write-Host "  -> megacores\MegaCore-1.zip" -ForegroundColor White
Write-Host "  -> megacores\MegaCore-2.zip" -ForegroundColor White
Write-Host "==================================================" -ForegroundColor Green
