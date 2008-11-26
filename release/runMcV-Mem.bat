@ECHO OFF

echo Reading system configuration...

set SYS_MEM=

for /f "tokens=4,5" %%i in ('systeminfo 2^>NUL ^|find "Total Physical Memory"') do set SYS_MEM_IN=x%%ix && set SYS_MEM_TYPE=%%j

:toint
set SYS_MEM_IN=%SYS_MEM_IN:~1%
set SYS_MEM_CHAR=%SYS_MEM_IN:~0,1%
if "%SYS_MEM_CHAR%" == "x" goto gotint
if "%SYS_MEM_CHAR%" == "," goto toint
set /a SYS_MEM=%SYS_MEM%%SYS_MEM_CHAR%
goto toint

:gotint
IF %SYS_MEM_TYPE% == GB SET /a SYS_MEM=%SYS_MEM% * 1024

REM Check if we are a 32bit or 64bit JRE and set SYS_MEM accordingly
set JRE_64=no
FOR /F %%i IN ('jre\bin\javaw.exe -version 2^>^&1 ^|find "64"') DO SET JRE_64=%%i
IF NOT %JRE_64%==no GOTO donemem
IF %SYS_MEM% GTR 1536 SET SYS_MEM=1536

:donemem