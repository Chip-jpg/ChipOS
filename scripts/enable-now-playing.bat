@echo off
rem Turns on the home screen music player. Run "adb connect <FIRE-TV-IP>:5555" first.
rem
rem Android only lets an app see what other apps are playing if it has an enabled notification
rem listener. ChipOS declares one, but never reads or stores notifications.
rem Undo with:  adb shell cmd notification disallow_listener app.chipos.launcher/app.chipos.launcher.NowPlayingListener
set LISTENER=app.chipos.launcher/app.chipos.launcher.NowPlayingListener

adb shell cmd notification allow_listener %LISTENER%

rem If that command is not available on this Fire OS, set the setting directly.
adb shell settings get secure enabled_notification_listeners | findstr /c:"app.chipos.launcher" >nul
if errorlevel 1 (
  echo The first method did not work, trying the settings method...
  adb shell settings put secure enabled_notification_listeners %LISTENER%
)

echo.
echo Enabled listeners now:
adb shell settings get secure enabled_notification_listeners
echo.
echo Done. Start some music (for example in Spotify), then look at ChipOS.
echo Settings ^> "Now playing card" shows what ChipOS can see, which helps if the player does not appear.
pause
