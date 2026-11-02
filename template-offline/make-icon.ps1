Add-Type -AssemblyName System.Drawing

$src = "assets\logo\logo.png"
if (-not (Test-Path $src)) {
    Write-Host "No logo found."
    exit 0
}

$bmp = [System.Drawing.Bitmap]::FromFile((Resolve-Path $src))
$size = 256
$square = New-Object System.Drawing.Bitmap $size, $size
$g = [System.Drawing.Graphics]::FromImage($square)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.Clear([System.Drawing.Color]::Transparent)

$ratio = [Math]::Min($size / $bmp.Width, $size / $bmp.Height)
$newW = [int]($bmp.Width * $ratio)
$newH = [int]($bmp.Height * $ratio)
$posX = [int](($size - $newW) / 2)
$posY = [int](($size - $newH) / 2)
$g.DrawImage($bmp, $posX, $posY, $newW, $newH)

$iconHandle = $square.GetHicon()
$icon = [System.Drawing.Icon]::FromHandle($iconHandle)
$dest = "assets\icons\app_icon.ico"
$fs = New-Object System.IO.FileStream($dest, [System.IO.FileMode]::Create)
$icon.Save($fs)
$fs.Close()

$bmp.Dispose()
$square.Dispose()
$g.Dispose()

Write-Host "App icon created: $(Test-Path $dest)"
