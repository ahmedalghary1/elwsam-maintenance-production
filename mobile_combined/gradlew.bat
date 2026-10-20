@rem Gradle startup script for Windows
@echo off
setlocal
chcp 65001 >NUL
if exist "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe" set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
set DIRNAME=%~dp0
if not defined JAVA_HOME if exist "%USERPROFILE%\android-build-tools\jdk17\bin\java.exe" set "JAVA_HOME=%USERPROFILE%\android-build-tools\jdk17"
if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_SDK_ROOT if defined ANDROID_HOME set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
echo ERROR: JAVA_HOME is not set and no java command could be found. 1>&2
exit /b 1
:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto execute
echo ERROR: JAVA_HOME points to an invalid directory: %JAVA_HOME% 1>&2
exit /b 1
:execute
if not exist "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" (
  echo ERROR: Missing Gradle wrapper JAR: "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" 1>&2
  exit /b 1
)
pushd "%DIRNAME%"
"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
popd
endlocal
