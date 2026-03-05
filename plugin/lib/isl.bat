@echo off
setlocal
REM ISL Command Line Runner for Windows (plugin lib - runs embedded JAR)
REM
REM Usage: isl.bat [command] [options]
REM Examples:
REM   isl.bat --version
REM   isl.bat info
REM   isl.bat transform script.isl -i input.json
REM   isl.bat test [path]

set SCRIPT_DIR=%~dp0
set JAR_FILE=%SCRIPT_DIR%isl-cmd-all.jar

if not exist "%JAR_FILE%" (
    echo Error: isl-cmd-all.jar not found in %SCRIPT_DIR%
    exit /b 1
)

java -jar "%JAR_FILE%" %*
endlocal
