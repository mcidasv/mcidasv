@echo off

REM Always run the default prefs; user can override as much as they want
CALL "runMcV-Prefs.bat"

REM Create .mcidasv directory if it doesn't already exist
IF NOT EXIST "%USERPROFILE%\.mcidasv" mkdir "%USERPROFILE%\.mcidasv"

REM Copy prefs to .mcidasv directory if it doesn't already exist
IF NOT EXIST "%USERPROFILE%\.mcidasv\runMcV-Prefs.bat" copy "runMcV-Prefs.bat" "%USERPROFILE%\.mcidasv\runMcV-Prefs.bat"

REM If .mcidas\runMcV-Prefs.bat exists, call it to populate the current environment
IF EXIST "%USERPROFILE%\.mcidasv\runMcV-Prefs.bat" CALL "%USERPROFILE%\.mcidasv\runMcV-Prefs.bat"

SET enable_3d=true
IF %USE_3DSTUFF%==0 SET enable_3d=false

SET MCV_FLAGS=-Didv.3d=%enable_3d%

REM Start McIDAS-V
jre\bin\java -Xmx%HEAP_SIZE% %D3DREND% -cp idv.jar -jar mcidasv.jar %MCV_FLAGS% %*