@echo off
setlocal EnableExtensions
chcp 65001 >NUL
title Maintenance Supervisor - Release APK Builder

cd /d "%~dp0"
if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe" set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"

set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "KEYTOOL=%JAVA_HOME%\bin\keytool.exe"
set "BUILD_TOOLS=%ANDROID_HOME%\build-tools\35.0.0"
set "ZIPALIGN=%BUILD_TOOLS%\zipalign.exe"
set "APKSIGNER=%BUILD_TOOLS%\apksigner.bat"
set "KEYSTORE=%~dp0.signing\maintenance-release.jks"
set "UNSIGNED_APK=%~dp0app\build\outputs\apk\release\app-release.apk"
set "TEMP_DIR=%~dp0.build-temp"
set "ALIGNED_APK=%~dp0.build-temp\maintenance-release-aligned.apk"
set "SIGNED_TEMP=%~dp0.build-temp\maintenance-supervisor-release.apk"
set "FINAL_APK=%~dp0release-output\maintenance-supervisor-release.apk"

echo ============================================================
echo Building signed Maintenance Supervisor APK
echo ============================================================

if not exist "%JAVA_EXE%" goto missing_java
if not exist "%ANDROID_HOME%\platforms\android-36\android.jar" goto missing_platform
if not exist "%ZIPALIGN%" goto missing_build_tools
if not exist "%APKSIGNER%" goto missing_build_tools
if not exist "%~dp0gradle\wrapper\gradle-wrapper.jar" goto missing_wrapper

if not exist "%~dp0.signing" mkdir "%~dp0.signing"
if not exist "%~dp0release-output" mkdir "%~dp0release-output"
if not exist "%TEMP_DIR%" mkdir "%TEMP_DIR%"

if not exist "%KEYSTORE%" (
    echo.
    echo A release signing key will now be created.
    echo Keep its password and this file safe for future updates:
    echo %KEYSTORE%
    echo.
    "%KEYTOOL%" -genkeypair -v -keystore "%KEYSTORE%" -alias maintenance -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Maintenance, O=Elwsam, C=EG" -storepass elwsam123 -keypass elwsam123
    if errorlevel 1 goto failed
)

echo.
echo [1/4] Checking Java...
"%JAVA_EXE%" -version
if errorlevel 1 goto failed

echo.
echo [2/4] Compiling optimized release APK...
call "%~dp0gradlew.bat" --no-daemon clean assembleRelease --stacktrace
if errorlevel 1 goto failed
if not exist "%UNSIGNED_APK%" goto missing_unsigned

echo.
echo [3/4] Aligning APK...
"%ZIPALIGN%" -p -f 4 "%UNSIGNED_APK%" "%ALIGNED_APK%"
if errorlevel 1 goto failed

echo.
echo [4/4] Signing and verifying APK...
echo Enter the release-key password when requested.
call "%APKSIGNER%" sign --ks "%KEYSTORE%" --ks-pass pass:elwsam123 --ks-key-alias maintenance --out "%SIGNED_TEMP%" "%ALIGNED_APK%"
if errorlevel 1 goto failed
call "%APKSIGNER%" verify --verbose --print-certs "%SIGNED_TEMP%"
if errorlevel 1 goto failed
move /y "%SIGNED_TEMP%" "%FINAL_APK%" >NUL
if errorlevel 1 goto failed
del /q "%ALIGNED_APK%" 2>NUL

echo.
echo ============================================================
echo SUCCESS
echo Final signed APK:
echo %FINAL_APK%
echo ============================================================
echo.
exit /b 0

:missing_java
echo ERROR: JDK 17 was not found at:
echo %JAVA_HOME%
goto failed

:missing_platform
echo ERROR: Android Platform 36 was not found at:
echo %ANDROID_HOME%\platforms\android-36
goto failed

:missing_build_tools
echo ERROR: Android Build Tools 35.0.0 were not found at:
echo %BUILD_TOOLS%
goto failed

:missing_wrapper
echo ERROR: gradle\wrapper\gradle-wrapper.jar is missing.
goto failed

:missing_unsigned
echo ERROR: Gradle finished but did not create:
echo %UNSIGNED_APK%
goto failed

:failed
echo.
echo BUILD FAILED. Review the error shown above.
echo No APK was installed or executed on this PC.
echo.
exit /b 1

