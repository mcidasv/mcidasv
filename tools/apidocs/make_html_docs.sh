#!/bin/sh

IDV_DIR="../../../IDV"
MCV_DIR="../../../mcidasv"
OUTPUT_DIR="${MCV_DIR}/dist"
APIDOCS_DIR="${MCV_DIR}/tools/apidocs"

HTML_OUTPUT_FILE="${OUTPUT_DIR}/jython.html"
HTML_FORMAT_SCRIPT="${APIDOCS_DIR}/testformatter.py"
MCV_CLASSPATH=".:${IDV_DIR}/libsrc/jythonlib.jar:${IDV_DIR}/lib/jython.jar"

CURRENT_DIR=`pwd`

cd ${APIDOCS_DIR}

mkdir -p ${OUTPUT_DIR}

$JAVA_HOME/bin/java -classpath "${MCV_CLASSPATH}" org.python.util.jython ${HTML_FORMAT_SCRIPT} > ${HTML_OUTPUT_FILE}

cd ${CURRENT_DIR}
