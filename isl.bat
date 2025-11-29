@echo off
REM ISL Command Line Runner for Windows
REM
REM This script allows you to run ISL commands without manually invoking Gradle
REM Usage: isl.bat [command] [options]
REM Examples:
REM   isl.bat --version
REM   isl.bat info
REM   isl.bat transform script.isl -i input.json

setlocal

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Read version from gradle.properties
for /f "tokens=1,* delims==" %%a in ('findstr "^version=" "%SCRIPT_DIR%gradle.properties"') do set VERSION=%%b

REM Check if the shadow JAR exists
set JAR_FILE=%SCRIPT_DIR%isl-cmd\build\libs\isl-%VERSION%.jar
if exist "%JAR_FILE%" (
    REM Use the pre-built JAR
    java -jar "%JAR_FILE%" %*
) else (
    REM Fall back to Gradle
    echo Shadow JAR not found. Building and running via Gradle...
    echo Run "gradlew.bat :isl-cmd:shadowJar" to build the JAR for faster startup.
    echo.
    cd /d "%SCRIPT_DIR%"
    call "%SCRIPT_DIR%gradlew.bat" :isl-cmd:run --quiet --console=plain --args="%*"
)

endlocal

