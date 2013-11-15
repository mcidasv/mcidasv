@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\McIDAS-V
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

REM Initialize new userpath
IF EXIST "%MCV_USERPATH%" GOTO hasuserpath

:inituserpath
IF EXIST "%USERPROFILE%\.mcidasv" (
	echo Moving files to new user path: %MCV_USERPATH%
	move "%USERPROFILE%\.mcidasv" "%MCV_USERPATH%"
)

:hasuserpath

SET LOGBACK_CONFIG="%MCV_USERPATH%/logback.xml"

set MCV_CLASSPATH=%CD%\;%CD%\mcv_userguide.jar;%CD%\sysout-over-slf4j-1.0.2.jar;%CD%\commons-math-2.2.jar;%CD%\log4j-over-slf4j-1.6.1.jar;%CD%\logback-classic-0.9.29.jar;%CD%\logback-core-0.9.29.jar;%CD%\miglayout-4.0-swing.jar;%CD%\slf4j-api-1.6.1.jar;%CD%\jython.jar;%CD%\eventbus-1.3.jar;%CD%\mcidasv.jar;%CD%\auxdata.jar;%CD%\external.jar;%CD%\local-idv.jar;%CD%\idv.jar;%CD%\local-visad.jar;%CD%\ncIdv.jar;%CD%\visad.jar

REM Get the amount of system memory
echo Reading system configuration...
SET SYS_MEM=0
FOR /F %%i IN ('jre\bin\java.exe -cp mcidasv.jar edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i

jre\bin\java -Dlogback.configurationFile=%LOGBACK_CONFIG% -classpath "%MCV_CLASSPATH%" -da edu.wisc.ssec.mcidasv.startupmanager.StartupManager -Didv.sysmem=%SYS_MEM% -userpath "%MCV_USERPATH%" %MCV_PARAMS%
