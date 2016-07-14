@ECHO OFF
SETLOCAL EnableDelayedExpansion
SETLOCAL EnableExtensions


SET SESSIONS=0

REM milliseconds to wait before closing welcome window
SET WELCOME_DELAY=2500

REM milliseconds to wait before closing mcv
SET MCV_DELAY=3000

SET USERPATH_PREFIX=!USERPROFILE!\hammermcv

SET SHOW_HELP=0

:checkparameters
IF '%1'=='' GOTO endparameters
IF '%1'=='-h' GOTO enablehelp
IF '%1'=='-help' GOTO enablehelp
IF '%1'=='-sessions' GOTO setsessions
IF '%1'=='-welcomedelay' GOTO setwelcomedelay
IF '%1'=='-mcvdelay' GOTO setmcvdelay
SHIFT
GOTO checkparameters

:enablehelp
SET SHOW_HELP=1
SHIFT
GOTO checkparameters

:setsessions
SET SESSIONS=%2
SHIFT
SHIFT
GOTO checkparameters

:setwelcomedelay
SET WELCOME_DELAY=%2
SHIFT
SHIFT
GOTO checkparameters

:setmcvdelay
SET MCV_DELAY=%2
SHIFT
SHIFT
GOTO checkparameters


REM finished parameter checking
:endparameters

IF "!SHOW_HELP!"=="1" (
    ECHO hammermcv.bat [OPTIONS]
    ECHO.
    ECHO   Execute runMcV.bat the specified number of times.
    ECHO.
    ECHO   Temporary userpaths will be created within !USERPATH_PREFIX! and
    ECHO   will be removed upon exit if no errors were encountered.
    ECHO.
    ECHO   Available Options:
    ECHO.
    ECHO     -h, -help: Show this message and exit.
    ECHO.
    ECHO     -sessions VALUE: Number of times to execute runMcV.bat Default: 0
    ECHO.
    ECHO     -welcomedelay VALUE: Milliseconds to wait before closing welcome window. Default: !WELCOME_DELAY!
    ECHO.
    ECHO     -mcvdelay VALUE: Milliseconds to wait before closing McV. Default: !MCV_DELAY!
    ECHO.
    GOTO done
)
    

REM finished everything related to argument processing, so show where we're at
:dostuff
ECHO.
ECHO Sessions to run: !SESSIONS!
ECHO Welcome Window delay: !WELCOME_DELAY! (ms)
ECHO McV delay: !MCV_DELAY! (ms)
ECHO.

REM variables that runMcV.bat will read
SET MCV_UNWELCOME_WINDOW="true"
SET MCV_UNWELCOME_DELAY="!WELCOME_DELAY!"
SET LOG_LEVEL="TRACE"

SET COUNT=0
SET KEEP_TEMPDIRS=1

:whileloop
IF !COUNT! LSS !SESSIONS! (
    IF NOT EXIST !USERPATH_PREFIX! (
        MKDIR "!USERPATH_PREFIX!"
    )
    
    SET tmpuserpath=!USERPATH_PREFIX!\temp_userpath_!COUNT!
    SET tmpargs=-Dmcidasv.autoexit.delay=!MCV_DELAY! -userpath "!tmpuserpath!" -doaction mcvautoquit
    SET /A COUNT += 1
    
    ECHO "Running: CALL runMcV.bat !tmpargs!"
    START /B /WAIT runMcV.bat !tmpargs!
    REM IF "!errorlevel!"=="1"
    REM     ECHO "*** Failed running: CALL runMcV.bat %tmpargs%"
    REM     ECHO "*** check out %tmpuserpath%"
    REM     SET KEEP_TEMPDIRS=1
    REM     GOTO endloop
    REM 
    GOTO whileloop
)

:endloop
IF "!KEEP_TEMPDIRS!"=="1" (
    ECHO.
    ECHO "*** cleaning up temporary userpaths in !USERPATH_PREFIX!"
    RMDIR /S /Q "!USERPATH_PREFIX!"
    
)
    
    
:done
ENDLOCAL
ENDLOCAL