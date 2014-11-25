#!/bin/sh

if [ -n "$1" ]; then
    
    DIR="$1"
    
    if [ -f "${DIR}/jre/bin/java" ]; then
        JAVA_PATH="${DIR}/jre/bin/java"
    else
        JAVA_PATH=$(which java)
    fi
    
    ${JAVA_PATH} -Xshare:dump >/dev/null 2>&1
    
    # make this explicit
    exit $?
fi
