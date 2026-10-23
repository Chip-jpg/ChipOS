@echo off
rem Asks Android not to restrict an app that plays music in the background (Spotify by default).
rem Run "adb connect <FIRE-TV-IP>:5555" first.
rem Usage:  scripts\keep-spotify-alive.bat [package-name]
rem
rem This removes Android's own background limits for the app. It cannot stop Fire OS from closing
rem the app when the stick runs out of memory (it has only 1 GB), so it helps but is not a guarantee.
rem Undo with:  adb shell cmd appops reset com.spotify.tv.android
set PKG=%~1
if "%PKG%"=="" set PKG=com.spotify.tv.android

adb shell cmd appops set %PKG% RUN_IN_BACKGROUND allow
adb shell cmd appops set %PKG% RUN_ANY_IN_BACKGROUND allow
adb shell dumpsys deviceidle whitelist +%PKG%
adb shell am set-standby-bucket %PKG% active
echo.
echo Done for %PKG%. If it says the package is unknown, install the app first.
pause
