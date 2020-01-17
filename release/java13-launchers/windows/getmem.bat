@ECHO OFF
REM This script is meant to be called by the McIDAS-V startup scripts,
REM and so it expects to be called from within the "lib" subdirectory.
..\jre\bin\java.exe --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -cp %MCV_JAR% edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL