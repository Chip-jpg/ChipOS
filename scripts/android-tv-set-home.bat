@echo off
rem Makes the Home button open ChipOS on an Android TV (for example a Sony Bravia) by disabling
rem the TV's own home screen for the current user. Nothing is deleted.
rem Run "adb connect <TV-IP>:5555" first, and install ChipOS with scripts\install-android-tv.bat.
rem
rem Undo at any time with scripts\android-tv-restore-home.bat, or by hand:
rem   adb shell pm enable com.google.android.tvlauncher
rem
rem Check the package name first:  adb shell pm list packages | findstr /i launcher
rem Only run this if "com.google.android.tvlauncher" is in that list.

adb shell pm list packages app.chipos.launcher | findstr /c:"app.chipos.launcher" >nul
if errorlevel 1 (
  echo ChipOS is not installed. Run scripts\install-android-tv.bat first.
  pause
  exit /b 1
)

adb shell pm list packages com.google.android.tvlauncher | findstr /c:"com.google.android.tvlauncher" >nul
if errorlevel 1 (
  echo com.google.android.tvlauncher was not found on this TV. Nothing was changed.
  echo Run: adb shell pm list packages ^| findstr /i launcher
  pause
  exit /b 1
)

adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
echo.
echo Done. Press Home on the remote: ChipOS should open. If Home or input switching misbehaves, run scripts\android-tv-restore-home.bat.
pause
