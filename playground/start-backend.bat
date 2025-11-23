@echo off
echo Starting ISL Playground Backend...
echo Backend will be available at: http://localhost:8080
echo.

cd /d "%~dp0backend"
call ..\..\gradlew.bat bootRun

