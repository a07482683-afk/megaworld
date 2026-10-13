@echo off
setlocal
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 not found. Install JDK 17 from https://adoptium.net and run again.
  pause
  exit /b 1
)
if not exist gradle-8.8\bin\gradle.bat (
  echo Downloading Gradle 8.8 ...
  powershell -NoProfile -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-8.8-bin.zip -OutFile gradle.zip; Expand-Archive gradle.zip -DestinationPath . -Force; Remove-Item gradle.zip"
)
call gradle-8.8\bin\gradle.bat build --no-daemon
if errorlevel 1 (
  echo BUILD FAILED. Send the text above to Claude.
  pause
  exit /b 1
)
echo.
echo DONE. Your mod: build\libs\megaworld-0.3.0.jar  (NOT the -sources one)
echo Copy it to your .minecraft\mods folder together with Fabric API.
pause
