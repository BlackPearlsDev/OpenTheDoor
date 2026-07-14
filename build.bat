@echo off
setlocal EnableDelayedExpansion
if not exist build mkdir build
javac -encoding UTF-8 -cp "libs/netty-all-4.1.68.Final.jar" -d build src\openthedoor\Main.java src\openthedoor\config\*.java src\openthedoor\detect\*.java src\openthedoor\log\*.java src\openthedoor\util\*.java src\openthedoor\scan\*.java src\openthedoor\ui\*.java src\openthedoor\net\tcp\*.java src\openthedoor\net\http\*.java src\openthedoor\net\proxy\*.java
if errorlevel 1 exit /b 1
if exist resources xcopy /E /I /Y resources build >nul
if errorlevel 1 exit /b 1

set "JAR_CMD=jar"
where jar >nul 2>nul
if errorlevel 1 (
  set "JAR_CMD="
  if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
  if not defined JAR_CMD (
    for /d %%D in ("%ProgramFiles%\Java\jdk*") do (
      if exist "%%~fD\bin\jar.exe" set "JAR_CMD=%%~fD\bin\jar.exe"
    )
  )
)

if not defined JAR_CMD (
  echo jar.exe not found. Install a JDK or add its bin directory to PATH.
  exit /b 1
)

"%JAR_CMD%" cfe OpenTheDoor.jar openthedoor.Main -C build .
if errorlevel 1 exit /b 1
echo Built OpenTheDoor.jar
