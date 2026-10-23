@echo off
rem Grants ChipOS the permissions its Settings screen can use. Run "adb connect <FIRE-TV-IP>:5555" first.
rem
rem ACCESS_FINE_LOCATION      Android only shows Wi-Fi and Bluetooth scan results to apps that hold it.
rem                           ChipOS never reads your location.
rem WRITE_SECURE_SETTINGS     Lets ChipOS change animation speed (Settings > Animation speed).
rem
rem Safe to run again. Undo with: adb shell pm revoke app.chipos.launcher <permission>
adb shell pm grant app.chipos.launcher android.permission.ACCESS_FINE_LOCATION
adb shell pm grant app.chipos.launcher android.permission.WRITE_SECURE_SETTINGS
echo.
echo Done. If you saw errors above, check "adb devices" shows your Fire TV as "device".
pause
