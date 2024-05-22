@ECHO OFF

SET MCV_USERPATH=%USERPROFILE%\McIDAS-V
SET MCV_LOGPATH="%MCV_USERPATH%\mcidasv.log"
SET USE_TEMPUSERPATH=0
SET WAIT_FOR_EXIT=1

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

REM If _JAVA_OPTIONS is set it takes precedence over command line
SET _JAVA_OPTIONS=

REM need to explicitly zero out Skip because batch files aren't run
REM in a subshell, so the value would be set to whatever the previous
REM invocation of runMcV set it to!
SET Skip=0

REM Check for our supported runMcV arguments, and then pass 'em
REM off to the appropriate argument handler (i.e. setuserpath, setlogpath, etc)
:checkparameters
IF '%1' == '' GOTO endparameters
IF '%1' == '-userpath' GOTO setuserpath
IF '%1' == '-logpath' GOTO setlogpath
IF '%1' == '-tempuserpath' GOTO settempuserpath
IF '%1' == '-guistart' GOTO setguistart
SHIFT
GOTO checkparameters

:setguistart
SET WAIT_FOR_EXIT=0
SHIFT
SET /A Skip += 1
GOTO checkparameters

:setuserpath
setlocal DisableDelayedExpansion
SET MCV_USERPATH=%~2
setlocal EnableDelayedExpansion
SHIFT
SHIFT
SET /A Skip += 2
GOTO checkparameters

:setlogpath
setlocal DisableDelayedExpansion
SET "MCV_LOGPATH=%~2"
setlocal EnableDelayedExpansion
SHIFT
SHIFT
SET /A Skip += 2
SET USER_SPECIFIED_LOGPATH="%MCV_LOGPATH%"
GOTO checkparameters

:settempuserpath
SET USE_TEMPUSERPATH=1
:maketempname
SET TEMPUSERPATH=%TMP%\mcidasv-%RANDOM%-%TIME:~6,5%.tmp
IF EXIST "%TEMPUSERPATH%" GOTO :maketempname
SHIFT
SET /A Skip += 1
GOTO checkparameters

:endparameters

REM the following crazy stuff is courtesy of https://stackoverflow.com/a/20702597
REM the problem is that the typical way of getting all parameters passed to
REM a batch file (aka "%*") apparently never changes, so all of the "SHIFT"
REM stuff in the above lines doesn't work.
REM another annoyance is that if Skip isn't incremented by one for each SHIFT
REM in the arg handlers, things will misbehave.

(
SETLOCAL ENABLEDELAYEDEXPANSION
FOR %%I IN (%*) DO IF !Skip! LEQ 0 ( 
        SET params=!params! %%I
    ) ELSE SET /A Skip-=1
)
(
ENDLOCAL
SET MCV_PARAMS=%params%
)

IF NOT "%MCV_USERPATH%"=="%USERPROFILE%\McIDAS-V" (
    SET MCV_LOGPATH="%MCV_USERPATH%\mcidasv.log"
)

IF "%USE_TEMPUSERPATH%"=="1" (
    SET MCV_USERPATH=%TEMPUSERPATH%
    SET MCV_LOGPATH=%TEMPUSERPATH%\mcidasv.log
    REM SET MCV_PARAMS=%MCV_PARAMS:-tempuserpath= %
    ECHO Using randomly generated userpath: %TEMPUSERPATH%
)

REM this check is done so that users specify a log file outside
REM of the McV user directory. Especially helpful with -tempuserpath!
IF DEFINED USER_SPECIFIED_LOGPATH (
    SET MCV_LOGPATH=%USER_SPECIFIED_LOGPATH%
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
SET MCV_LOG="%MCV_USERPATH%\mcidasv.log"
SET MCV_LOG_LINES=10000

REM Always run the default prefs; user can override as much as they want
IF NOT EXIST "..\runMcV-Prefs.bat" echo "This script must be run from within the McIDAS-V installation directory" && goto cleanup
CALL "..\runMcV-Prefs.bat"

REM Toggle the welcome window if MCV_USERPATH does not exist
IF NOT EXIST "%MCV_USERPATH%" SET SHOW_WELCOME=1

REM Create MCV_USERPATH directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%" mkdir "%MCV_USERPATH%"

REM Copy prefs to MCV_USERPATH directory if it doesn't already exist
IF NOT EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" COPY "..\runMcV-Prefs.bat" "%MCV_USERPATH%\runMcV-Prefs.bat"

REM If MCV_USERPATH\runMcV-Prefs.bat exists, call it to populate the current environment
IF EXIST "%MCV_USERPATH%\runMcV-Prefs.bat" CALL "%MCV_USERPATH%\runMcV-Prefs.bat"

SET ENABLE_3D=true
IF %USE_3DSTUFF%==0 SET ENABLE_3D=false

echo %MCV_PARAMS% | findstr /C:" -script ">nul && (
    SET BACKGROUND=1
) || (
    set BACKGROUND=0
)

REM disable ability to change garbage collector (for now?)
SET GC_ARGS=

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

REM control dark mode
IF "%USE_DARK_MODE%"=="1" (
set DARK_MODE=true
) ELSE (
SET DARK_MODE=false
)

SET LOGBACK_CONFIG="%MCV_USERPATH%\logback.xml"

SET MCV_FLAGS=-Didv.3d=%ENABLE_3D% -Dvisad.java3d.noerasebackground=true -Dvisad.java3d.textureNpot=%ALLOW_NPOT% -Dvisad.java3d.imageByRef=%IMAGE_BY_REF% -Dvisad.java3d.geometryByRef=%GEOMETRY_BY_REF% -DtextureWidthMax=%TEXTURE_SIZE% -forceaqua -userpath "%MCV_USERPATH%"

REM the unwelcome stuff is used for stress testing mcv startup
IF DEFINED MCV_UNWELCOME_DELAY (
SET MCV_FLAGS=%MCV_UNWELCOME_DELAY% %MCV_FLAGS%
)

IF DEFINED MCV_UNWELCOME_WINDOW (
SET MCV_FLAGS=-autoquit %MCV_FLAGS%
)

REM Toggle the welcome window if MCV_USERPATH does not exist
IF NOT EXIST "%MCV_USERPATH%" SET SHOW_WELCOME=1

REM Show the welcome window if needed
IF "%SHOW_WELCOME%"=="1" (
SET MCV_FLAGS=-welcomewindow %MCV_FLAGS%
)

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
REM Get the amount of system memory
echo Reading system configuration...
SET /a SYS_MEM=0
FOR /F %%i IN ('..\jre\bin\java.exe -cp %MCV_JAR% edu.wisc.ssec.mcidasv.util.GetMem 2^>NUL') DO SET SYS_MEM=%%i
IF %SYS_MEM% LEQ 0 SET HEAP_SIZE=%HEAP_DEFAULT% && ECHO UHOH && GOTO goodheap
set HEAP_PERCENT=%HEAP_SIZE:~0,-1%
set /a HEAP_SIZE=%SYS_MEM% * %HEAP_PERCENT% / 100
set HEAP_SIZE=%HEAP_SIZE%M

:goodheap

:startup
REM Start McIDAS-V

IF EXIST "..\jre\bin\client\classes.jsa" (
@echo *** notice: using class data sharing
)

set JPMS_MONSTROSITY=--add-exports java.base/jdk.internal.loader=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.swing=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/javax.security.auth.x500=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.security.cert=ALL-UNNAMED --add-opens java.base/java.security.spec=ALL-UNNAMED --add-opens java.base/javax.net=ALL-UNNAMED --add-opens java.base/javax.net.ssl=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.nio.channels=ALL-UNNAMED --add-opens java.base/java.nio.charset=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.util.concurrent=ALL-UNNAMED --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens java.base/java.util.concurrent.locks=ALL-UNNAMED --add-opens java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.security.jgss/org.ietf.jgss=ALL-UNNAMED --add-opens java.desktop/java.awt=ALL-UNNAMED --add-opens java.desktop/java.awt.geom=ALL-UNNAMED --add-opens java.desktop/javax.swing=ALL-UNNAMED --add-opens java.desktop/javax.swing.plaf=ALL-UNNAMED --add-opens java.desktop/javax.swing.plaf.basic=ALL-UNNAMED --add-opens java.desktop/javax.accessibility=ALL-UNNAMED --add-opens java.desktop/sun.java2d.opengl=ALL-UNNAMED --add-opens java.naming/javax.naming.ldap=ALL-UNNAMED --add-opens java.sql/java.sql=ALL-UNNAMED

@echo Command line: "..\jre\bin\javaw.exe" "-Xmx%HEAP_SIZE%" %GC_ARGS% %JVM_ARGS% -Dfile.encoding=UTF-8 -Dpython.security.respectJavaAccessibility=false -Dmcidasv.darkmode=%DARK_MODE% -Dloglevel=%LOGGING_LEVEL% -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -Dmcv.logpath=%MCV_LOGPATH% -jar %MCV_JAR% %MCV_FLAGS% %MCV_PARAMS%

IF DEFINED MCV_UNWELCOME_WINDOW (
    IF "%WAIT_FOR_EXIT%"=="1" (
        "..\jre\bin\java.exe" "-Xmx%HEAP_SIZE%" %GC_ARGS% %JPMS_MONSTROSITY% %JVM_ARGS% -Dfile.encoding=UTF-8 -Dpython.security.respectJavaAccessibility=false -Dmcidasv.darkmode=%DARK_MODE% -Dloglevel=%LOGGING_LEVEL% -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -Dmcv.logpath=%MCV_LOGPATH% -jar %MCV_JAR% %MCV_FLAGS% %MCV_PARAMS%
        GOTO cleanup
    ) ELSE (
        start /B "McIDAS-V" "..\jre\bin\javaw.exe" "-Xmx%HEAP_SIZE%" %GC_ARGS% %JPMS_MONSTROSITY% %JVM_ARGS% -Dfile.encoding=UTF-8 -Dpython.security.respectJavaAccessibility=false -Dmcidasv.darkmode=%DARK_MODE% -Dloglevel=%LOGGING_LEVEL% -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -Dmcv.logpath=%MCV_LOGPATH% -jar %MCV_JAR% %MCV_FLAGS% %MCV_PARAMS%
    )
) ELSE (
    IF "%WAIT_FOR_EXIT%"=="1" (
        "..\jre\bin\java.exe" "-Xmx%HEAP_SIZE%" %GC_ARGS% %JPMS_MONSTROSITY% %JVM_ARGS% -Dfile.encoding=UTF-8 -Dpython.security.respectJavaAccessibility=false -Dmcidasv.darkmode=%DARK_MODE% -Dloglevel=%LOGGING_LEVEL% -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -Dmcv.logpath=%MCV_LOGPATH% -jar %MCV_JAR% %MCV_FLAGS% %MCV_PARAMS%
        GOTO cleanup
    ) ELSE (
        start /B "McIDAS-V" "..\jre\bin\javaw.exe" "-Xmx%HEAP_SIZE%" %GC_ARGS% %JPMS_MONSTROSITY% %JVM_ARGS% -Dfile.encoding=UTF-8 -Dpython.security.respectJavaAccessibility=false -Dmcidasv.darkmode=%DARK_MODE% -Dloglevel=%LOGGING_LEVEL% -Dlogback.configurationFile=%LOGBACK_CONFIG% -Dmcv.userpath="%MCV_USERPATH%" -Dmcv.logpath=%MCV_LOGPATH% -jar %MCV_JAR% %MCV_FLAGS% %MCV_PARAMS%
    )
)

:cleanup
endlocal

:end
