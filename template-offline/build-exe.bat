@echo off
echo ==================================================
echo    Game Show Center Offline - Generador de .EXE   
echo ==================================================
powershell -ExecutionPolicy Bypass -File "%~dp0build-exe.ps1"
pause
