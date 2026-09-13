@echo off
setlocal
set "ROOT=%~dp0.."
if "%~1"=="" (set "TARGET=forge-1.20.1") else (set "TARGET=%~1" & shift)
if "%MC_JAVA_HOME_21%"=="" if not "%JAVA_HOME%"=="" set "MC_JAVA_HOME_21=%JAVA_HOME%"
if "%MC_JAVA_HOME_21%"=="" (
  echo Set MC_JAVA_HOME_21 or JAVA_HOME to JDK 21 or newer. 1>&2
  exit /b 2
)
if not exist "%MC_JAVA_HOME_21%\bin\java.exe" (
  echo Java executable not found: %MC_JAVA_HOME_21%\bin\java.exe 1>&2
  exit /b 2
)
call "%ROOT%\gradlew.bat" :tools:target-launcher:installDist --no-daemon
if errorlevel 1 exit /b %ERRORLEVEL%
rem %* keeps the original argument list after SHIFT, so forward the shifted
rem arguments explicitly to avoid passing the target id as a Gradle task.
call "%ROOT%\tools\target-launcher\dist\bin\target-launcher.bat" %TARGET% %~1 %~2 %~3 %~4 %~5 %~6 %~7 %~8 %~9
exit /b %ERRORLEVEL%
