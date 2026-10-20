@echo off
setlocal
call gradlew.bat :app:assembleRelease
if errorlevel 1 exit /b %errorlevel%
if not exist release-output mkdir release-output
copy /Y app\build\outputs\apk\release\app-release.apk release-output\elwsam-operations-release.apk
echo APK: %CD%\release-output\elwsam-operations-release.apk
