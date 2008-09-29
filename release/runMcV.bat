@echo off

set MCV_USERPATH=%USERPROFILE%\.mcidasv
set MCV_PARAMS=%*

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

SET enable_3d=true
IF %USE_3DSTUFF%==0 SET enable_3d=false

SET MCV_FLAGS=-Didv.3d=%enable_3d%

REM Start McIDAS-V
jre\bin\java -Xmx%HEAP_SIZE% %D3DREND% -cp idv.jar -jar mcidasv.jar %MCV_FLAGS% %MCV_PARAMS%
