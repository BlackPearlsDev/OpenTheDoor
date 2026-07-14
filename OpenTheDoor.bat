@echo off
setlocal
cd /d "%~dp0"

if not exist OpenTheDoor.jar (
  echo OpenTheDoor.jar not found. Building it now...
  call build.bat
  if errorlevel 1 (
    pause
    exit /b 1
  )
)

java -jar OpenTheDoor.jar
