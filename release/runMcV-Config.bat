@ECHO OFF

REM Get the amount of system memory
echo Reading system configuration...
SET SYS_MEM=0
FOR /F %%i IN ('jre\bin\java.exe -cp mcidasv.jar edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i

jre\bin\java -jar startupmanager.jar -Didv.sysmem=%SYS_MEM% %*
