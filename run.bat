@echo off
title ClassRoll
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo Install a JDK first. Then double-click run.bat again.
  pause
  exit /b 1
)

if not exist out mkdir out
echo Compiling...
javac -encoding UTF-8 -cp "lib\mysql-connector-j-8.0.33.jar;lib\json-20230227.jar;lib\jakarta.mail-2.0.1.jar;lib\jakarta.activation-2.0.1.jar" -d out src\attendance\*.java
if errorlevel 1 (
  echo.
  echo Compile failed. See the messages above.
  pause
  exit /b 1
)

echo Starting ClassRoll...
java -cp "out;lib\mysql-connector-j-8.0.33.jar;lib\json-20230227.jar;lib\jakarta.mail-2.0.1.jar;lib\jakarta.activation-2.0.1.jar" attendance.Server
echo.
pause
