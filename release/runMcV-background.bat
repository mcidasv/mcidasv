@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\McIDAS-V
SET MCV_PARAMS=%*

SET MCV_JAR=
FOR /F %%a IN ('DIR /b mcidasv-*.jar 2^>nul') DO SET MCV_JAR=%%a
IF DEFINED MCV_JAR (
    GOTO finduserguide
) ELSE (
    ECHO "*** ERROR: Could not find McIDAS-V JAR file"
    GOTO end
)

:finduserguide
SET USERGUIDE_JAR=
FOR /F %%b IN ('DIR /b mcv_userguide-*.jar 2^>nul') DO SET USERGUIDE_JAR=%%b
IF DEFINED USERGUIDE_JAR (
    GOTO donefindingjars
) ELSE (
    ECHO "*** ERROR: Could not find McIDAS-V User's Guide JAR file"
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

REM Default heap size to use if none can be determined
SET HEAP_DEFAULT=512M

REM Controls whether or not the welcome window appears (0 = no, 1 = yes)
SET SHOW_WELCOME=0

REM Put the log files in the user's MCV_USERPATH directory (which should be writeable)
SET MCV_LOG=%MCV_USERPATH%\mcidasv.log
SET MCV_LOG_LINES=10000

REM Always run the default prefs; user can override as much as they want
IF NOT EXIST runMcV-Prefs.bat echo This script must be run from within the McIDAS-V installation directory && goto end
CALL runMcV-Prefs.bat

REM Toggle the welcome window if MCV_USERPATH does not exist
IF NOT EXIST "%MCV_USERPATH%" SET SHOW_WELCOME=1

REM Create MCV_USERPATH directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%" mkdir "%MCV_USERPATH%"

REM Copy prefs to MCV_USERPATH directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" COPY runMcV-Prefs.bat "%MCV_USERPATH%\runMcV-Prefs.bat"

REM If MCV_USERPATH\runMcV-Prefs.bat exists, call it to populate the current environment
IF EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" CALL "%MCV_USERPATH%\runMcV-Prefs.bat"

SET ENABLE_3D=true
IF %USE_3DSTUFF%==0 SET ENABLE_3D=false

REM Determine whether or not Mcv should use Direct3D
IF "%D3DREND%"=="1" (
SET D3D_FLAG=-Dj3d.rend=d3d
) ELSE (
SET D3D_FLAG=
)

REM Show the welcome window if needed
if "%SHOW_WELCOME%"=="1" (
jre\bin\javaw.exe -Dmcv.userpath="%MCV_USERPATH%" -cp %MCV_JAR% edu.wisc.ssec.mcidasv.util.WelcomeWindow
if ERRORLEVEL 1 GOTO end
)

REM Used to set the garbage collector. keeping around because it may be use in the future.
SET JVM_ARGS=

REM temp?: toggles the visad.java3d.geometryByRef property
IF "%USE_GEOBYREF%"=="0" (
SET GEOMETRY_BY_REF=false
) ELSE (
SET GEOMETRY_BY_REF=true
)

REM toggles the visad.java3d.imageByRef property
IF "%USE_IMAGEBYREF%"=="0" (
SET IMAGE_BY_REF=false
) ELSE (
SET IMAGE_BY_REF=true
)

REM toggles the visad.java3d.textNpot property
IF "%USE_NPOT%"=="0" (
SET ALLOW_NPOT=false
) ELSE (
SET ALLOW_NPOT=true
)
SET LOGBACK_CONFIG="%MCV_USERPATH%\logback.xml"

REM Get the amount of system memorys
echo Reading system configuration...
SET /a SYS_MEM=0
FOR /F %%i IN ('jre\bin\java.exe -cp %MCV_JAR% edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i

SET MCV_FLAGS=-Didv.3d=%ENABLE_3D% -Didv.sysmem=%SYS_MEM% -Dvisad.java3d.textureNpot=%ALLOW_NPOT% -Dvisad.java3d.imageByRef=%IMAGE_BY_REF% -Dvisad.java3d.geometryByRef=%GEOMETRY_BY_REF% -userpath "%MCV_USERPATH%"

REM Append the specified startup bundle to the args getting passed to Mcv
IF DEFINED STARTUP_BUNDLE SET MCV_FLAGS=%MCV_FLAGS% -bundle %STARTUP_BUNDLE%

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
IF "%LAST_CHAR%" == "p" GOTO percentheap
IF "%LAST_CHAR%" == "P" GOTO percentheap
IF "%LAST_CHAR%" == "%%" GOTO percentheap
set HEAP_SIZE=%HEAP_SIZE%M
GOTO goodheap

:percentheap
IF %SYS_MEM% LEQ 0 SET HEAP_SIZE=%HEAP_DEFAULT% && GOTO goodheap
set HEAP_PERCENT=%HEAP_SIZE:~0,-1%
set /a HEAP_SIZE=%SYS_MEM% * %HEAP_PERCENT% / 100
set HEAP_SIZE=%HEAP_SIZE%M

:goodheap

:startup
REM Start McIDAS-V

IF EXIST "jre\bin\client\classes.jsa" (
@echo *** notice: using class data sharing
) ELSE (
@echo *** notice: not using class data sharing
)

set MCV_CLASSPATH=%CD%\%MCV_JAR%;%CD%\%USERGUIDE_JAR%

@echo Command line: jre\bin\java.exe -XX:MaxPermSize=128m -Xmx%HEAP_SIZE% %JVM_ARGS% %D3D_FLAG% -Dpython.security.respectJavaAccessibility=false -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -classpath "%MCV_CLASSPATH%" -da edu.wisc.ssec.mcidasv.McIDASV %MCV_FLAGS% %MCV_PARAMS%

jre\bin\java.exe -XX:MaxPermSize=128m -Xmx%HEAP_SIZE% %JVM_ARGS% %D3D_FLAG% -Dpython.security.respectJavaAccessibility=false -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -classpath "%MCV_CLASSPATH%" -da edu.wisc.ssec.mcidasv.McIDASV %MCV_FLAGS% %MCV_PARAMS%

:end
