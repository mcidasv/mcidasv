#!/bin/sh

#
# $Id$
#

DEST_DIR=/home/mcidasv/idv/lib
URL=ftp://ftp.unidata.ucar.edu/pub/idv/nightly
FILE=`curl -ls ${URL}/ |grep idv_jars |xargs echo`

cd $DEST_DIR
wget ${URL}/${FILE} && unzip -o ${FILE} && rm -f ${FILE}
cd -
