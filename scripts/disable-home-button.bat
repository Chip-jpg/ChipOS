@echo off
rem Turns the ChipOS Home button interception off again.
adb shell settings delete secure enabled_accessibility_services
adb shell settings put secure accessibility_enabled 0
echo.
echo Home goes back to Amazon's home screen.
pause
