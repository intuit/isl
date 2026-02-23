@echo off
REM Example usage scripts for ISL CLI on Windows

echo === ISL CLI Examples ===
echo.

REM Read version from gradle.properties
for /f "tokens=1,2 delims==" %%a in (..\..\gradle.properties) do (
    if "%%a"=="version" set VERSION=%%b
)

REM Check if JAR exists
set JAR=..\build\libs\isl-%VERSION%.jar
if not exist "%JAR%" (
    echo Building ISL CLI...
    cd ..
    call gradlew.bat shadowJar
    cd examples
)

set ISL=java -jar %JAR%

echo 1. Simple Hello World
echo    Command: isl transform hello.isl -i hello-input.json --pretty
%ISL% transform hello.isl -i hello-input.json --pretty
echo.
echo.

echo 2. Data Transformation
echo    Command: isl transform transform.isl -i data.json --pretty
%ISL% transform transform.isl -i data.json --pretty
echo.
echo.

echo 3. With Command-Line Parameters
echo    Command: isl transform hello.isl -p name=Alice --pretty
%ISL% transform hello.isl -p name=Alice --pretty
echo.
echo.

echo 4. Validate Script
echo    Command: isl validate transform.isl
%ISL% validate transform.isl
echo.
echo.

echo 5. Show Info
echo    Command: isl info
%ISL% info
echo.


