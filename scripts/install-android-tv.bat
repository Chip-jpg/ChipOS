@echo off
rem Installs (or updates) ChipOS on an Android TV (for example a Sony Bravia), then starts it.
rem Usage:  scripts\install-android-tv.bat [path\to\app.apk]
rem Run "adb connect <TV-IP>:5555" first, and accept the prompt on the TV.
rem
rem This does NOT change the Home button. Run scripts\android-tv-set-home.bat for that,
rem after you have checked that ChipOS works. Undo with scripts\android-tv-restore-home.bat.
set APK=%~1
if "%APK%"=="" set APK=app\build\outputs\apk\release\app-release.apk
if not exist "%APK%" set APK=app\build\outputs\apk\debug\app-debug.apk

if not exist "%APK%" (
  echo APK not found: %APK%
  echo Build it first:  .\gradlew.bat assembleRelease  (PowerShell)
  pause
  exit /b 1
)

echo Installing %APK%
adb install -r "%APK%"
if errorlevel 1 (
  echo Install failed. Check "adb devices" shows your TV as "device".
  echo If it says the signature does not match, uninstall the old ChipOS first:  adb uninstall app.chipos.launcher
  pause
  exit /b 1
)

call "%~dp0android-tv-permissions.bat" nopause
echo Optimizing ChipOS for speed (takes a few seconds)...
adb shell cmd package compile -m speed -f app.chipos.launcher
adb shell am start -n app.chipos.launcher/.MainActivity
echo.
echo ChipOS installed and started. The TV's own home screen is still enabled. System sounds were turned on.
pause
