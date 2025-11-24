@echo off
echo Starting ISL Playground Frontend...
echo Frontend will be available at: http://localhost:3000
echo.

cd /d "%~dp0frontend"

if not exist "node_modules" (
  echo Installing dependencies...
  call npm install
)

call npm run dev

