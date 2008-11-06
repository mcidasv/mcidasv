@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\.mcidasv
SET MCV_PARAMS=%*

SET MCV_OUTPUT=mcv_output.log
SET MCV_ERROR=mcv_error.log

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

REM Always run the default prefs; user can override as much as they want
CALL "runMcV-Prefs.bat"

REM Create .mcidasv directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%" mkdir "%MCV_USERPATH%"

REM Copy prefs to .mcidasv directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" copy "runMcV-Prefs.bat" "%MCV_USERPATH%\runMcV-Prefs.bat"

REM If .mcidas\runMcV-Prefs.bat exists, call it to populate the current environment
IF EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" CALL "%MCV_USERPATH%\runMcV-Prefs.bat"

SET ENABLE_3D=true
IF %USE_3DSTUFF%==0 SET ENABLE_3D=false

SET MCV_FLAGS=-Didv.3d=%ENABLE_3D%

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
set HEAP_SIZE=%HEAP_SIZE%M

:goodheap
REM Start McIDAS-V

echo ################ 1>>%MCV_OUTPUT%
date /t 1>>%MCV_OUTPUT%
time /t 1>>%MCV_OUTPUT%

echo ################ 1>>%MCV_ERROR%
date /t 1>>%MCV_ERROR%
time /t 1>>%MCV_ERROR%

start /B jre\bin\javaw.exe -Xmx%HEAP_SIZE% %D3DREND% -cp idv.jar -jar mcidasv.jar %MCV_FLAGS% %MCV_PARAMS% 1>>%MCV_OUTPUT% 2>>%MCV_ERROR%
