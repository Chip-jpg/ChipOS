@echo off
rem Installs (or updates) ChipOS on a connected Fire TV, then starts it.
rem Usage:  scripts\install.bat [path\to\app.apk]
rem Run "adb connect <FIRE-TV-IP>:5555" first.
rem
rem Prefer the RELEASE build: it is much faster than debug. In Android Studio:
rem   Build ^> Generate App Bundles or APKs ^> Generate APKs ^> choose "release"
rem or from a terminal:  .\gradlew.bat assembleRelease  (PowerShell)
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
  echo Install failed. Check "adb devices" shows your Fire TV as "device".
  echo If it says the signature does not match, uninstall the old ChipOS first:  adb uninstall app.chipos.launcher
  pause
  exit /b 1
)

adb shell pm grant app.chipos.launcher android.permission.ACCESS_FINE_LOCATION
adb shell pm grant app.chipos.launcher android.permission.WRITE_SECURE_SETTINGS
adb shell cmd notification allow_listener app.chipos.launcher/app.chipos.launcher.NowPlayingListener
adb shell am start -n app.chipos.launcher/.MainActivity
echo.
echo ChipOS installed and started. Amazon's launcher is still enabled; nothing else was changed.
pause
