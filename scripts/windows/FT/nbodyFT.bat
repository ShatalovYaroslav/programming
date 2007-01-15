@echo off

echo Starting Fault-Tolerant version of ProActive NBody...

rem --- Verifying current directory
SET COMMAND=%0
IF NOT "%COMMAND:~-4%" == ".bat" (
 SET COMMAND=%0.bat
)
 
SET OK=0
FOR /F %%i in ('dir /b') do IF "%%i" == "%COMMAND%" SET OK=1

IF %OK% == 0 (
echo scripts must be started in the same directory as the script.
goto end
)

SETLOCAL
IF NOT DEFINED PROACTIVE set PROACTIVE=%CD%\..\..\..

OLDDIR=%CD%

CD ..

call nbody.bat -displayft 4 3000

CD "%OLDIR%"

ENDLOCAL