@echo off
REM Run this file from the SAME folder as VMS.exe after installation.
REM Typical layout: this .bat next to VMS.exe, with subfolders "runtime" and "app".
set "DIR=%~dp0"
set "JAVA=%DIR%runtime\bin\java.exe"
set "MODPATH=%DIR%app"
if not exist "%JAVA%" (
  echo ERROR: Java runtime not found at:
  echo %JAVA%
  echo Edit this script if your install layout is different.
  pause
  exit /b 1
)
echo Starting VMS with module path: %MODPATH%
"%JAVA%" -p "%MODPATH%" -m pkg.vms/pkg.vms.Bootstrap
echo Exit code: %ERRORLEVEL%
pause
