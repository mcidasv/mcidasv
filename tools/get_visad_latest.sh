#!/bin/sh

#
# $Id$
#

DEST_DIR=/home/mcidasv/idv/lib
URL=ftp://ftp.ssec.wisc.edu/pub/visad/nightly
FILE=visad.jar

cd $DEST_DIR
wget ${URL}/${FILE} -O ${FILE}
cd -
