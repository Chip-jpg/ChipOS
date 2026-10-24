@echo off
rem Puts the Android TV's own home screen back (undo for scripts\android-tv-set-home.bat).
rem Run "adb connect <TV-IP>:5555" first.
adb shell pm enable com.google.android.tvlauncher
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
echo.
echo Done. The TV's own home screen is enabled again.
pause
