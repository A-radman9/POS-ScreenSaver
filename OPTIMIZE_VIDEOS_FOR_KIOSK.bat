@echo off
title POS Kiosk Video Optimizer
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ======================================================================
echo          POS KIOSK AD VIDEO OPTIMIZER (FOR ROCKCHIP RK3576)
echo ======================================================================
echo.
echo Checking environment...

if not exist "Input_Ads_Here" mkdir "Input_Ads_Here"
if not exist "Optimized_Ads_For_Kiosk" mkdir "Optimized_Ads_For_Kiosk"

python --version >nul 2>&1
if %ERRORLEVEL% equ 0 (
    python "%~dp0tools\optimize_videos.py" %*
) else (
    echo [!] Python not found in PATH. Running directly with ffmpeg...
    if exist "%~dp0tools\ffmpeg.exe" (
        for %%F in ("%~dp0Input_Ads_Here\*.*") do (
            echo Converting: %%~nxF
            "%~dp0tools\ffmpeg.exe" -i "%%F" -vf "scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:color=black" -c:v libx264 -preset fast -crf 22 -pix_fmt yuv420p -r 30 -c:a aac -movflags +faststart -y "%~dp0Optimized_Ads_For_Kiosk\%%~nF_kiosk.mp4"
        )
        echo.
        echo Done! Check the folder: Optimized_Ads_For_Kiosk
        pause
    ) else (
        echo [ERROR] tools\ffmpeg.exe not found!
        pause
    )
)
