#!/bin/sh

#
# $Id$
#

# This script will send the necessary Windows server components to
# mcidasv@pappy (to be used with pack_servers.sh)

################################################################################

# Where to put the .zip package
DEST_DIR=mcidasv@pappy:/home/mcidasv/adde

# Binary server files
BIN_FILES="
mcserv.exe
mcservl.exe
lwprserv.exe
txtgserv.exe
adirserv.exe agetserv.exe
areaadir.exe areaaget.exe
fsdxadir.exe fsdxaget.exe
gvaradir.exe gvaraget.exe
lv1badir.exe lv1baget.exe
mod4adir.exe mod4aget.exe
modradir.exe modraget.exe
modsadir.exe modsaget.exe
modxadir.exe modxaget.exe
msgtadir.exe msgtaget.exe
msgxadir.exe msgxaget.exe
omtpadir.exe omtpaget.exe
poesadir.exe poesaget.exe

amseadir.exe amseaget.exe
amsradir.exe amsraget.exe
amrradir.exe amrraget.exe
sminadir.exe sminaget.exe
tminadir.exe tminaget.exe

airsadir.exe airsaget.exe
awipadir.exe awipaget.exe
giniadir.exe giniaget.exe
mod8adir.exe mod8aget.exe
mtstadir.exe mtstaget.exe

vpserv.exe
atokserv.exe
mdfhserv.exe
mdhdserv.exe
mdksserv.exe
mdroserv.exe
"

# Data files
DATA_FILES="
MODIS.CORE
SATBAND
"

################################################################################

# Send BIN_FILES
BIN_TAR_FILE=mcv_windows_bin.tar
cd ~/mcidas/bin
tar -cvf ${BIN_TAR_FILE} ${BIN_FILES}
scp ${BIN_TAR_FILE} ${DEST_DIR} && rm ${BIN_TAR_FILE}

# Send DATA_FILES
DATA_TAR_FILE=mcv_windows_data.tar
cd ~/mcidas/data
tar -cvf ${DATA_TAR_FILE} ${DATA_FILES}
scp ${DATA_TAR_FILE} ${DEST_DIR} && rm ${DATA_TAR_FILE}

# Tell the user what to do next
echo "${BIN_TAR_FILE} and ${DATA_TAR_FILE} sent to ${DEST_DIR}"
