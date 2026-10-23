@echo off
rem Copies your own photos to the Fire TV Stick so ChipOS can use them as wallpapers.
rem Usage: drag a folder of photos (.jpg .jpeg .png .webp) onto this file, or run:
rem        push-wallpapers.bat C:\Path\To\Photos
rem Needs "adb connect <FIRE-TV-IP>:5555" first. Then in ChipOS: Settings > Wallpapers > My photos.
set DEST=/sdcard/Android/data/app.chipos.launcher/files/wallpapers

if "%~1"=="" (
  echo Drag a folder of photos onto this file, or run: push-wallpapers.bat C:\Path\To\Photos
  pause
  exit /b 1
)
if not exist "%~1\" (
  echo "%~1" is not a folder.
  pause
  exit /b 1
)

adb shell mkdir -p %DEST%
adb push "%~1\." %DEST%/
echo.
echo Done. Open ChipOS Settings and set Wallpapers to "My photos" or "Both".
pause
