@ECHO OFF

SET CURRENT_DIR="%CD%"

IF NOT EXIST "lib" echo This script must be run from within the McIDAS-V installation directory && goto end
setlocal
CD lib

SET MCV_JAR=
FOR /F %%a IN ('DIR /b mcidasv-*.jar 2^>nul') DO SET MCV_JAR=%%a
IF DEFINED MCV_JAR (
    GOTO donefindingjars
) ELSE (
    ECHO "*** ERROR: Could not find McIDAS-V JAR file"
    GOTO cleanup
)

:donefindingjars

start /B "Java3D Test" "..\jre\bin\javaw.exe" -classpath %MCV_JAR% edu.wisc.ssec.mcidasv.util.J3dTest

endlocal

:end
