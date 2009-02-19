@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\.mcidasv
SET MCV_PARAMS=%*

REM Check for -userpath parameter
:checkparameters
IF '%1' == '' GOTO endparameters
IF '%1' == '-userpath' GOTO setuserpath
SHIFT
GOTO checkparameters

:setuserpath
SET MCV_USERPATH=%2
SHIFT
SHIFT
GOTO checkparameters

:endparameters

REM Default heap size to use if none can be determined
SET HEAP_DEFAULT=512M

REM Put the log files in the user's .mcidasv directory (which should be writeable)
SET MCV_LOG=%MCV_USERPATH%\mcidasv.log
SET MCV_LOG_LINES=10000

REM Always run the default prefs; user can override as much as they want
IF NOT EXIST runMcV-Prefs.bat echo This script must be run from within the McIDAS-V installation directory && goto end
CALL runMcV-Prefs.bat

REM Create .mcidasv directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%" mkdir "%MCV_USERPATH%"

REM Copy prefs to .mcidasv directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" copy runMcV-Prefs.bat "%MCV_USERPATH%\runMcV-Prefs.bat"

REM If .mcidas\runMcV-Prefs.bat exists, call it to populate the current environment
IF EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" CALL "%MCV_USERPATH%\runMcV-Prefs.bat"

SET ENABLE_3D=true
IF %USE_3DSTUFF%==0 SET ENABLE_3D=false

REM Determine whether or not Mcv should use Direct3D
IF "%D3DREND%"=="1" (
SET D3D_FLAG=-Dj3d.rend=d3d
) ELSE (
SET D3D_FLAG=
)

REM Get the amount of system memory
set /a SYS_MEM=0
IF EXIST runMcV-Mem.bat CALL runMcV-Mem.bat

SET MCV_FLAGS=-Didv.3d=%ENABLE_3D% -Didv.sysmem=%SYS_MEM%

REM Append the specified startup bundle to the args getting passed to Mcv
IF DEFINED STARTUP_BUNDLE SET MCV_FLAGS=%MCV_FLAGS% -bundle %STARTUP_BUNDLE%

REM Check for valid HEAP_SIZE
SET LAST_CHAR=%HEAP_SIZE:~-1%
IF "%LAST_CHAR%" == "b" GOTO goodheap
IF "%LAST_CHAR%" == "B" GOTO goodheap
IF "%LAST_CHAR%" == "k" GOTO goodheap
IF "%LAST_CHAR%" == "K" GOTO goodheap
IF "%LAST_CHAR%" == "m" GOTO goodheap
IF "%LAST_CHAR%" == "M" GOTO goodheap
IF "%LAST_CHAR%" == "g" GOTO goodheap
IF "%LAST_CHAR%" == "G" GOTO goodheap
IF "%LAST_CHAR%" == "t" GOTO goodheap
IF "%LAST_CHAR%" == "T" GOTO goodheap
IF "%LAST_CHAR%" == "p" GOTO percentheap
IF "%LAST_CHAR%" == "P" GOTO percentheap
IF "%LAST_CHAR%" == "%%" GOTO percentheap
set HEAP_SIZE=%HEAP_SIZE%M
GOTO goodheap

:percentheap
IF %SYS_MEM% LEQ 0 SET HEAP_SIZE=%HEAP_DEFAULT% && GOTO goodheap
set HEAP_PERCENT=%HEAP_SIZE:~0,-1%
set /a HEAP_SIZE=%SYS_MEM% * %HEAP_PERCENT% / 100
set HEAP_SIZE=%HEAP_SIZE%M

:goodheap
REM Clean the log file to last MCV_LOG_LINES lines
IF NOT EXIST "%MCV_LOG%" GOTO startup
for /f "tokens=2 delims=:" %%i in ('find /v /c "" "%MCV_LOG%"') do set /a LINES=%%i 2>NUL
if %LINES% GTR 0 GOTO gotcount
for /f "tokens=3 delims=:" %%i in ('find /v /c "" "%MCV_LOG%"') do set /a LINES=%%i 2>NUL

:gotcount
if %LINES% LEQ %MCV_LOG_LINES% GOTO startup
set MCV_TEMP=%MCV_USERPATH%\mcidasv.tmp
set /a START=%LINES% - %MCV_LOG_LINES%
more /e +%START% "%MCV_LOG%" > "%MCV_TEMP%"
erase /f "%MCV_LOG%"
rename "%MCV_TEMP%" mcidasv.log

:startup
REM Start McIDAS-V

@echo ################ >>"%MCV_LOG%"
date /t >>"%MCV_LOG%"
time /t >>"%MCV_LOG%"
@echo Command line: jre\bin\javaw.exe -Xmx%HEAP_SIZE% %D3D_FLAG% -cp idv.jar -jar mcidasv.jar %MCV_FLAGS% %MCV_PARAMS% >>"%MCV_LOG%"

start /B jre\bin\javaw.exe -Xmx%HEAP_SIZE% %D3D_FLAG% -cp idv.jar -jar mcidasv.jar %MCV_FLAGS% %MCV_PARAMS% 2>>"%MCV_LOG%"

:end
