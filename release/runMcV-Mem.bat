@ECHO OFF

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