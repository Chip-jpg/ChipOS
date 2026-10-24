@echo off
rem Undo for scripts\android-tv-disable-ads.bat. Run "adb connect <TV-IP>:5555" first.
for %%P in (tv.samba.ssm com.sony.dtv.promos com.sony.dtv.sonyselect) do (
  echo Enabling %%P
  adb shell pm enable %%P
)
echo.
echo Done.
pause
