@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\McIDAS-V
SET MCV_PARAMS=%*

SET MCV_DIR=%~dp0%

SET CURRENT_DIR=%cd%

CD %MCV_DIR%\lib

SET MCV_JAR=
FOR /F %%a IN ('DIR /b mcidasv-*.jar 2^>nul') DO SET MCV_JAR=%%a
IF DEFINED MCV_JAR (
    GOTO donefindingjars
) ELSE (
    ECHO "*** ERROR: Could not find McIDAS-V JAR file"
    GOTO end
)

:donefindingjars

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

REM Initialize new userpath
IF EXIST "%MCV_USERPATH%" GOTO hasuserpath

:inituserpath
IF EXIST "%USERPROFILE%\.mcidasv" (
	echo Moving files to new user path: %MCV_USERPATH%
	move "%USERPROFILE%\.mcidasv" "%MCV_USERPATH%"
)

:hasuserpath

SET LOGBACK_CONFIG="%MCV_USERPATH%\logback.xml"

set MCV_CLASSPATH=%MCV_JAR%;%USERGUIDE_JAR%

REM Get the amount of system memory
echo Reading system configuration...
SET SYS_MEM=0
FOR /F %%i IN ("%MCV_DIR%\jre\bin\java.exe" -cp %MCV_JAR% edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i

"%MCV_DIR%\jre\bin\java" -Dmcv.userpath="%MCV_USERPATH%" -Dlogback.configurationFile=%LOGBACK_CONFIG% -classpath "%MCV_CLASSPATH%" -da edu.wisc.ssec.mcidasv.startupmanager.StartupManager -Didv.sysmem=%SYS_MEM% -userpath "%MCV_USERPATH%" %MCV_PARAMS%
:end

CD %CURRENT_DIR%