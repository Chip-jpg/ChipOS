@echo off
rem Makes ChipOS open when you press Home on the remote.
rem Fire OS always opens Amazon's home screen on Home and cannot change that without root. This turns
rem on ChipOS's small Accessibility service, which notices Amazon's home screen opening and opens
rem ChipOS instead. It only sees which app's window opened, never your screen's content. Android
rem delays this by about 5 seconds after Home is pressed, and Amazon's screen shows meanwhile.
rem Run "adb connect <FIRE-TV-IP>:5555" first, and install ChipOS before this.
rem Undo: scripts\disable-home-button.bat

adb shell settings put secure enabled_accessibility_services app.chipos.launcher/app.chipos.launcher.HomeKeyService
adb shell settings put secure accessibility_enabled 1
echo.
echo Done. Open another app and press Home: ChipOS appears after a few seconds.
echo You may need to run this again after reinstalling ChipOS.
pause
