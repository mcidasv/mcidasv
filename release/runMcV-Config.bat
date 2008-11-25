@ECHO OFF

REM Get the amount of system memory
set /a SYS_MEM=0
IF EXIST runMcV-Mem.bat CALL runMcV-Mem.bat

jre\bin\java -jar startupmanager.jar -Didv.sysmem=%SYS_MEM% %*
