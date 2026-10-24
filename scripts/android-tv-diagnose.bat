@echo off
rem Collects everything needed to tailor ChipOS to this TV into tv-info.txt. Changes nothing.
rem Run "adb connect <TV-IP>:5555" first, then:  .\scripts\android-tv-diagnose.bat
set OUT=tv-info.txt
echo ChipOS TV diagnostics > %OUT%

echo. >> %OUT%
echo == Device == >> %OUT%
adb shell getprop ro.product.manufacturer >> %OUT%
adb shell getprop ro.product.model >> %OUT%
adb shell getprop ro.build.version.release >> %OUT%
adb shell getprop ro.build.version.sdk >> %OUT%
adb shell getprop ro.build.version.security_patch >> %OUT%

echo. >> %OUT%
echo == Screen == >> %OUT%
adb shell wm size >> %OUT%
adb shell wm density >> %OUT%

echo. >> %OUT%
echo == Current home app == >> %OUT%
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME >> %OUT%

echo. >> %OUT%
echo == Launcher / home packages == >> %OUT%
adb shell pm list packages | findstr /i "launcher home" >> %OUT%

echo. >> %OUT%
echo == Sony, Samba, ads, recommendation packages == >> %OUT%
adb shell pm list packages | findstr /i "sony samba advert ads recommend tvrecommend promo" >> %OUT%

echo. >> %OUT%
echo == Google TV packages == >> %OUT%
adb shell pm list packages | findstr /i "com.google.android.tv com.google.android.katniss com.google.android.leanback" >> %OUT%

echo. >> %OUT%
echo == Apps you can launch (dock candidates) == >> %OUT%
adb shell pm list packages | findstr /i "youtube netflix spotify dazn amazon disney sky espn rai mediaset" >> %OUT%

echo.
echo Done. Send me the contents of %OUT% (in the folder you ran this from).
pause
