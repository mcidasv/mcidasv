#!/bin/bash

# This script will create the source .zip file that ultimately gets put on the
# web server for download
################################################################################

MCV_DIR="/home/mcidasv/mcidasv"
MEDIA_DIR="/home/mcidasv/install4j/media"
CUR_DIR="${PWD}"

# Get the version
VERSION=$(cat ${MCV_DIR}/release/mcidasv.install4j  |\
	grep "<application name.*version" |\
	sed -e 's/.*version=\"//' -e 's/\".*//')

NAME=McIDAS-V_${VERSION}_source
PACK_DIR="../../source/${NAME}"

echo "Packing McIDAS-V ${VERSION} source"

# Clean up
echo "Cleaning up"
rm -Rf ${PACK_DIR} && mkdir -p ${PACK_DIR} && cd ${PACK_DIR}
if [ $? -ne 0 ]; then
	echo "Failed to change to ${PACK_DIR}"
	exit 1
fi

# Get McIDAS-V source
echo "Copying McIDAS-V source..."
mkdir ./mcidasv
cp ${MCV_DIR}/build.xml ./mcidasv/
for SUBDIR in docs edu lib mcidas release lib ucar visad; do
	echo "  ${SUBDIR}" && cp -R ${MCV_DIR}/${SUBDIR} ./mcidasv/
done

# Clean it up
echo "Removing .class files"
find . -name "*.class" |xargs rm -f

echo "Removing javadoc"
rm -Rf ./mcidasv/docs/javadoc

echo "Removing webstart"
rm -Rf ./mcidasv/release/webstart

echo "Removing PDF guide"
rm -f ./mcidasv/docs/userguide/processed/mcv_guide.pdf

# Pack it up
echo "Zipping..."
cd ..
rm -f ${NAME}.zip && zip -r ${NAME}.zip ${NAME}
echo "Created $PWD/${NAME}.zip"

echo "Returning to original directory: ${CUR_DIR}"
cd ${CUR_DIR}

# Copy it to the install4j/media directory
echo "Copying to ${MEDIA_DIR}"
cp "../../source/${NAME}.zip" "${MEDIA_DIR}"
