#!/bin/bash

export MCV_UNWELCOME_WINDOW="true"

SESSIONS=0

# in milliseconds
WELCOME_DELAY=2500

# in milliseconds
MCV_DELAY=3000

USERPATH_PREFIX="/tmp/hammermcv"

SHOW_HELP=0

if [ $# -gt 1 ]; then
    
    while [ "$1" != "" ]; do
        if [ "$1" = "-sessions" ]; then
            SESSIONS="$2"
            shift
            shift
        elif [ "$1" = "-welcomedelay" ]; then
            WELCOME_DELAY="$2"
            shift
            shift
        elif [ "$1" = "-mcvdelay" ]; then
            MCV_DELAY="$2"
            shift
            shift
        elif [ "$1" = "-h" ] || [ "$1" = "-help" ]; then
            SHOW_HELP=1
            shift
        fi
    done
else
    SHOW_HELP=1
fi

if [ "${SHOW_HELP}" -eq 1 ]; then
    echo "$0 [OPTIONS]"
    echo "  "
    echo "  Execute runMcV the specified number of times."
    echo "  "
    echo "  Temporary userpaths will be created within ${USERPATH_PREFIX} and"
    echo "  will be removed upon exit if no errors were encountered."
    echo "  "
    echo "  Available Options:"
    echo "  "
    echo "    -h, -help: Show this message and exit."
    echo "    "
    echo "    -sessions: Number of times to execute runMcV. Default: 0"
    echo "    "
    echo "    -welcomedelay: Milliseconds to wait before closing welcome window. Default: ${WELCOME_DELAY}"
    echo "    "
    echo "    -mcvdelay: Milliseconds to wait before closing McV. Default: ${MCV_DELAY}"
    echo "    "
    exit 0
fi

echo ""
echo "Sessions to run: ${SESSIONS}"
echo "Welcome Window delay: ${WELCOME_DELAY} (ms)"
echo "McV delay: ${MCV_DELAY} (ms)"
echo ""

export MCV_UNWELCOME_DELAY="${WELCOME_DELAY}"
export LOG_LEVEL="TRACE"

count=0
keep_tempdirs=1
while [ $count -lt "${SESSIONS}" ]; do
    if [ ! -d "${USERPATH_PREFIX}" ]; then
        mkdir -p "${USERPATH_PREFIX}"
    fi
    userpath="${USERPATH_PREFIX}/temp_userpath_${count}"
    count=$((count+1))
    
    run_mcv="./runMcV -Dmcidasv.autoexit.delay=${MCV_DELAY} -userpath ${userpath} -doaction autoquit"
    
    echo "running: ${run_mcv}"
    eval "${run_mcv}"
    if [ $? -ne 0 ]; then
        echo "*** Failed running: ${run_mcv}"
        echo "*** check out ${userpath}!"
        count="$SESSIONS"
        keep_tempdirs=1
    fi
done

if [ $keep_tempdirs -eq 0 ] && [ -d "${USERPATH_PREFIX}" ]; then
    echo ""
    echo "*** cleaning up temporary userpaths in ${USERPATH_PREFIX}"
    rm -rf "${USERPATH_PREFIX}"
fi