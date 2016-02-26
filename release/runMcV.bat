@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\McIDAS-V
SET MCV_LOGPATH=%MCV_USERPATH%\mcidasv.log
SET MCV_PARAMS=%*
SET USE_TEMPUSERPATH=0

REM If _JAVA_OPTIONS is set it takes precedence over command line
SET _JAVA_OPTIONS=

REM Check for -userpath parameter
:checkparameters
IF '%1' == '' GOTO endparameters
IF '%1' == '-userpath' GOTO setuserpath
IF '%1' == '-logpath' GOTO setlogpath
IF '%1' == '-tempuserpath' GOTO settempuserpath
SHIFT
GOTO checkparameters

:setuserpath
setlocal DisableDelayedExpansion
SET "MCV_USERPATH=%~2"
setlocal EnableDelayedExpansion
SHIFT
SHIFT
GOTO checkparameters

:setlogpath
setlocal DisableDelayedExpansion
SET "MCV_LOGPATH=%~2"
setlocal EnableDelayedExpansion
SHIFT
SHIFT
GOTO checkparameters

:settempuserpath
SET USE_TEMPUSERPATH=1
:maketempname
SET TEMPUSERPATH=%TMP%\mcidasv-%RANDOM%-%TIME:~6,5%.tmp
IF EXIST "%TEMPUSERPATH%" GOTO :maketempname
SHIFT
GOTO checkparameters

:endparameters

IF NOT "%MCV_USERPATH%"=="%USERPROFILE%\McIDAS-V" (
    SET MCV_LOGPATH="%MCV_USERPATH%\mcidasv.log"
)

IF "%USE_TEMPUSERPATH%"=="1" (
    SET MCV_USERPATH=%TEMPUSERPATH%
    SET MCV_LOGPATH=%TEMPUSERPATH%\mcidasv.log
    SET MCV_PARAMS=%MCV_PARAMS:-tempuserpath= %
    ECHO Using randomly generated userpath: %TEMPUSERPATH%
)

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

REM Show the welcome window if needed
if "%SHOW_WELCOME%"=="1" (
jre\bin\javaw.exe -Dmcv.userpath="%MCV_USERPATH%" -cp mcidasv.jar edu.wisc.ssec.mcidasv.util.WelcomeWindow
if ERRORLEVEL 1 GOTO end
)

REM temp: toggles the CMS collector
IF "%USE_CMSGC%"=="1" (
SET GC_ARGS=-J-XX:+UseConcMarkSweepGC -J-XX:+CMSClassUnloadingEnabled
) ELSE (
SET GC_ARGS=
)

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

REM control textureWidthMax value
IF DEFINED TEXTURE_WIDTH (
SET TEXTURE_SIZE=%TEXTURE_WIDTH%
) ELSE (
SET TEXTURE_SIZE=4096
)

REM if the user has disabled loading layout.mcv we need to pass -nodefault
if "%DEFAULT_LAYOUT%"=="0" (
SET USE_LAYOUT_BUNDLE=-nodefault
) ELSE (
SET USE_LAYOUT_BUNDLE=
)

SET LOGBACK_CONFIG="%MCV_USERPATH%\logback.xml"

REM Get the amount of system memorys
echo Reading system configuration...
SET /a SYS_MEM=0
FOR /F %%i IN ('jre\bin\java.exe -cp mcidasv.jar edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i

SET MCV_FLAGS=-J-Didv.3d=%ENABLE_3D% -J-Didv.sysmem=%SYS_MEM% -J-Dvisad.java3d.textureNpot=%ALLOW_NPOT% -J-Dvisad.java3d.imageByRef=%IMAGE_BY_REF% -J-Dvisad.java3d.geometryByRef=%GEOMETRY_BY_REF% -J-DtextureWidthMax=%TEXTURE_SIZE% -userpath "%MCV_USERPATH%"

REM Append the specified startup bundle to the args getting passed to Mcv
IF NOT DEFINED STARTUP_BUNDLE GOTO endbundle

SET MCV_FLAGS=%MCV_FLAGS% %USE_LAYOUT_BUNDLE% -bundle %STARTUP_BUNDLE%

:endbundle

REM control mcidasv root logger level
IF DEFINED LOG_LEVEL (
SET LOGGING_LEVEL=%LOG_LEVEL%
) ELSE (
SET LOGGING_LEVEL=INFO
)

REM allow the user to supply args to the JVM. caveat emptor!
IF DEFINED JVM_OPTIONS (
SET JVM_ARGS=%JVM_OPTIONS%
) ELSE (
SET JVM_ARGS=
)

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

set MCV_CLASSPATH=%CD%\;%CD%\mcv_userguide.jar;%CD%\mcidasv.jar

set MCV_EXTPATH=-Djava.ext.dirs="jre\lib\ext"
set MCV_LIBPATH=-Djava.library.path="jre\lib\ext"

@echo Command line: mcidasv.exe -Xmx%HEAP_SIZE% %GC_ARGS% %JVM_ARGS% %MCV_EXTPATH% %MCV_LIBPATH% -Dpython.security.respectJavaAccessibility=false -Dloglevel=%LOGGING_LEVEL% -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -Dmcv.logpath="%MCV_LOGPATH%" -da edu.wisc.ssec.mcidasv.McIDASV %MCV_FLAGS% %MCV_PARAMS%

start /B mcidasv.exe -J-Xmx%HEAP_SIZE% %GC_ARGS% %JVM_ARGS% %MCV_EXTPATH% %MCV_LIBPATH% -J-Dpython.security.respectJavaAccessibility=false -J-Dloglevel=%LOGGING_LEVEL% -J-Dlogback.configurationFile=%LOGBACK_CONFIG% -J-Dmcv.userpath="%MCV_USERPATH%" -J-Dmcv.logpath="%MCV_LOGPATH%" %MCV_FLAGS% %MCV_PARAMS%

:end
