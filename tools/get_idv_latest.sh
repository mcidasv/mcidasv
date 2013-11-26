#!/bin/sh

#
# $Id$
#

# DEST_DIR=/home/mcidasv/idv/lib
# BACK_DIR=/home/mcidasv/idv/lib.old
DEST_DIR=../../IDV/lib
BACK_DIR=../../IDV/lib.old

URL=ftp://ftp.unidata.ucar.edu/pub/idv/nightly

FILE=`curl -ls ${URL}/ |grep idv_jars |xargs echo`

if [ -d ${BACK_DIR} ]; then
	echo "Old backup of IDV libraries exists."
	echo -n "Overwrite with current copy? [y|N] "
	read CONTYN
	if [ "${CONTYN}" = "y" -o "${CONTYN}" = "Y" ]; then
		echo "Making new backup of current IDV libraries..."
		rm -Rf ${BACK_DIR} && mv ${DEST_DIR} ${BACK_DIR} && mkdir ${DEST_DIR}
	fi
fi

echo "Getting new IDV nightly libraries..."
export OLDPWD=${PWD}
mkdir -p ${DEST_DIR}
cd ${DEST_DIR}
curl -O ${URL}/${FILE} && unzip -o ${FILE} && rm -f ${FILE}
cd ${OLDPWD}

SELF=`hostname`
echo "IDV JAR files updated on ${SELF}; will appear in the next McIDAS-V nightly build." |\
	mail -s "AUTO: New IDV JAR files" mug.team@ssec.wisc.edu
