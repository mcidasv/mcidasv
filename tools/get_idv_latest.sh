#
# $Id$
#

FTP=ftp.unidata.ucar.edu
DEST_DIR=../lib
VER=2.4b1

FILE=idv_jars_$VER.zip
URL=ftp://$FTP/pub/idv/nightly_idv_$VER

CMD="wget $URL/$FILE"

cd $DEST_DIR
$CMD && unzip -o idv_jars_$VER.zip 
cd -