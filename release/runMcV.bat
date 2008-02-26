REM Always run the default prefs; user can override as much as they want
CALL runMcV-Prefs.bat

REM Create .mcidasv directory if it doesn't already exist
IF NOT EXIST %HOME%\.mcidasv mkdir %HOME%\.mcidasv

REM Copy prefs to .mcidasv directory if it doesn't already exist
IF NOT EXIST %HOME%\.mcidasv\runMcV-Prefs.bat copy runMcV-Prefs.bat %HOME%\.mcidasv\runMcV-Prefs.bat

REM If .mcidas\runMcV-Prefs.bat exists, call it to populate the current environment
IF EXIST %HOME%\.mcidasv\runMcV-Prefs.bat CALL runMcV-Prefs.bat

REM Start McIDAS-V
jre\bin\java -Xmx%HEAP_SIZE% %D3DREND% -cp idv.jar -jar mcidasv.jar %*
