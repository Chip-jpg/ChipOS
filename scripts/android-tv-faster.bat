@echo off
rem Makes the TV's own animations twice as fast, so everything feels quicker.
rem Undo with:  scripts\android-tv-faster.bat normal
rem Run "adb connect <TV-IP>:5555" first.
set SCALE=0.5
if /i "%~1"=="normal" set SCALE=1
adb shell settings put global window_animation_scale %SCALE%
adb shell settings put global transition_animation_scale %SCALE%
adb shell settings put global animator_duration_scale %SCALE%
echo Animation scale set to %SCALE%.
