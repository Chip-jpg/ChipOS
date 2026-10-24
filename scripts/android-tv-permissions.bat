@echo off
rem Sets up every permission ChipOS uses on an Android TV, then shows what is in effect.
rem Run "adb connect <TV-IP>:5555" first, and install ChipOS before this. Safe to run again.
rem Run it again after reinstalling ChipOS if something stops working.
rem
rem   Location               Android only shows Wi-Fi and Bluetooth scan results to apps that hold it.
rem                          ChipOS never reads your location.
rem   Write secure settings  Lets ChipOS change the animation speed.
rem   Notification listener  Lets ChipOS see what is playing (the "now playing" card).
rem                          It never reads or stores notifications.
rem   System sounds          The TV's own click sounds, which ChipOS uses for navigation.
rem
rem Everything else ChipOS asks for (internet, Wi-Fi, Bluetooth, boot, uninstall) is granted at install.
rem Undo the listener with:
rem   adb shell cmd notification disallow_listener app.chipos.launcher/app.chipos.launcher.NowPlayingListener
set PKG=app.chipos.launcher
set LISTENER=%PKG%/%PKG%.NowPlayingListener

adb shell pm list packages %PKG% | findstr /c:"%PKG%" >nul
if errorlevel 1 (
  echo ChipOS is not installed. Run scripts\install-android-tv.bat first.
  if /i not "%~1"=="nopause" pause
  exit /b 1
)

adb shell pm grant %PKG% android.permission.ACCESS_FINE_LOCATION
adb shell pm grant %PKG% android.permission.WRITE_SECURE_SETTINGS

adb shell cmd notification allow_listener %LISTENER%
rem If that command is not available, set the setting directly (keeping any listeners already there).
adb shell settings get secure enabled_notification_listeners | findstr /c:"%PKG%" >nul
if errorlevel 1 (
  echo The first method did not work, trying the settings method...
  for /f "delims=" %%L in ('adb shell settings get secure enabled_notification_listeners') do set CURRENT=%%L
  if /i "%CURRENT%"=="null" (set CURRENT=) else if "%CURRENT%"=="" (set CURRENT=) else set CURRENT=%CURRENT%:
  adb shell settings put secure enabled_notification_listeners "%CURRENT%%LISTENER%"
)

adb shell settings put system sound_effects_enabled 1

echo.
echo === What is in effect ===
adb shell dumpsys package %PKG% | findstr /c:"ACCESS_FINE_LOCATION: granted" /c:"WRITE_SECURE_SETTINGS: granted"
echo Notification listeners:
adb shell settings get secure enabled_notification_listeners
echo System sounds (1 = on):
adb shell settings get system sound_effects_enabled
echo.
echo Done. Both permission lines above should say granted=true, and the listener list should contain %PKG%.
echo Start some music (for example in Spotify), then look at ChipOS.
if /i not "%~1"=="nopause" pause
