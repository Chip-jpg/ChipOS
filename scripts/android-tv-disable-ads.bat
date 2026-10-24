@echo off
rem Turns off the Sony Bravia services that track viewing or show promotions. Nothing is deleted.
rem Run "adb connect <TV-IP>:5555" first.
rem
rem   tv.samba.ssm              Samba (viewing-data / interactive TV service)
rem   com.sony.dtv.promos       Sony promotions
rem   com.sony.dtv.sonyselect   Sony Select (recommended apps and content)
rem
rem If a line says "Unknown package" or "not allowed", that service is protected on this firmware
rem and was left alone. Undo everything with scripts\android-tv-restore-ads.bat.
rem A Sony system update can switch some of these back on; run this again afterwards.
for %%P in (tv.samba.ssm com.sony.dtv.promos com.sony.dtv.sonyselect) do (
  echo Disabling %%P
  adb shell pm disable-user --user 0 %%P
)
echo.
echo Done. Also turn off, in the TV settings: Privacy ^> Ads personalization, and any Samba / Interactive TV option.
pause
