#!/bin/sh

#
# $Id$
#

# This script will create an OpenADDE distribution (in .zip file format)
# from the specified dmake build directory

################################################################################

# Binary server files
BIN_FILES="
adirserv
agetserv
airsadir
airsaget
areaadir
areaaget
fsdxadir
fsdxaget
giniadir
giniaget
gvaradir
gvaraget
lv1badir
lv1baget
lwprserv
mcserv
mcservl
mod4adir
mod4aget
mod8adir
mod8aget
modradir
modraget
modsadir
modsaget
modxadir
modxaget
msgtadir
msgtaget
msgxadir
msgxaget
mtstadir
mtstaget
omtpadir
omtpaget
poesadir
poesaget
txtgserv

amsradir
amsraget
awipadir
awipaget
sminadir
sminaget
tminadir
tminaget
"

# Data files
DATA_FILES="
MODIS.CORE
SATBAND
"

# List of available platforms
#PLATFORMS="
#aix
#darwin
#darwin86
#hpux
#irix
#linux
#linux64
#solaris
#soli86
#"

PLATFORMS="
darwin
darwin86
linux
solaris
soli86
windows
"

# List of available dmake sources
SRC_DIRS="
tester
tester1
tester2
tester3
"

# Where to create the .zip package
DEST_DIR=/home/mcidasv/adde

################################################################################

# Pick the source directory for packaging
echo "Select source directory:"
echo -n "  eg:"
for EACH in ${SRC_DIRS}; do
	echo -n " ${EACH}"
done
echo ""
echo -n "Selection: "
read SRC_DIR_CHOICE
if [ ! -d "/nas/home/${SRC_DIR_CHOICE}" ]; then
	echo "ERROR: /nas/home/${SRC_DIR_CHOICE} does not exist"
	exit 1
fi
SRC_DIR_FULL="/nas/home/${SRC_DIR_CHOICE}"

# Pick the platform to package
echo "Select platform:"
echo -n "  eg:"
for EACH in ${PLATFORMS}; do
	echo -n " ${EACH}"
done
echo ""
echo -n "Selection: "
read PLATFORM_CHOICE
if [ ! -d "${SRC_DIR_FULL}/mcidas/${PLATFORM_CHOICE}" -a \
		"${PLATFORM_CHOICE}" != "windows" ]; then
	echo "ERROR: ${SRC_DIR_FULL}/mcidas/${PLATFORM_CHOICE} does not exist"
	exit 1
fi
PLATFORM_FULL=${SRC_DIR_FULL}/mcidas/${PLATFORM_CHOICE}

################################################################################

# Special windows section:
# In this case, look for adde-windows.zip and verify that it contains all the
#  necessary executables
if [ "${PLATFORM_CHOICE}" = "windows" ]; then

if [ ! -f "${DEST_DIR}/adde-windows.zip" ]; then
	echo "ERROR: ${DEST_DIR_PLAT}/adde-windows.zip does not exist"
	exit 1
fi
cd ${DEST_DIR}
LIST=$(unzip -l adde-windows.zip |awk '{print $4}' |xargs -i{} basename {})
for FILE in ${LIST}; do
	FOUND=0
	if [ \
		"${FILE}" = "Name" -o \
		"${FILE}" = "----" -o \
		"${FILE}" = "adde" -o \
		"${FILE}" = "bin" -o \
		"${FILE}" = "data" \
	]; then
		continue;
	fi

	for CYG_FILE in cygwin1.dll; do
		if [ "${FILE}" = "${CYG_FILE}" ]; then
			FOUND=1
			break
		fi
	done
	for BIN_FILE in ${BIN_FILES}; do
		if [ "${FILE}" = "${BIN_FILE}.exe" ]; then
			FOUND=1
			break
		fi
	done
	for DATA_FILE in ${DATA_FILES}; do
		if [ "${FILE}" = "${DATA_FILE}" ]; then
			FOUND=1
			break
		fi
	done
	if [ ${FOUND} -eq 0 ]; then
		echo "${FILE} doesn't need to be there"
	fi
done

for CYG_FILE in cygwin1.dll; do
	FOUND=0
	for FILE in ${LIST}; do
		if [ "${FILE}" = "${CYG_FILE}" ]; then
			FOUND=1
			break
		fi
	done
	if [ ${FOUND} -eq 0 ]; then
		echo "${CYG_FILE} is missing"
	fi
done

for BIN_FILE in ${BIN_FILES}; do
	FOUND=0
	for FILE in ${LIST}; do
		if [ "${FILE}" = "${BIN_FILE}.exe" ]; then
			FOUND=1
			break
		fi
	done
	if [ ${FOUND} -eq 0 ]; then
		echo "${BIN_FILE}.exe is missing"
	fi
done

for DATA_FILE in ${DATA_FILES}; do
	FOUND=0
	for FILE in ${LIST}; do
		if [ "${FILE}" = "${DATA_FILE}" ]; then
			FOUND=1
			break
		fi
	done
	if [ ${FOUND} -eq 0 ]; then
		echo "${DATA_FILE} is missing"
	fi
done

exit 0
fi
# End special windows section

################################################################################

# Start packaging process
SRC_DIR_BIN=${PLATFORM_FULL}/bin
SRC_DIR_DATA=${SRC_DIR_FULL}/mcidas/data
DEST_DIR_PLAT=${DEST_DIR}/${PLATFORM_CHOICE}
DEST_DIR_ROOT=${DEST_DIR_PLAT}/adde
DEST_DIR_BIN=${DEST_DIR_ROOT}/bin
DEST_DIR_DATA=${DEST_DIR_ROOT}/data

# Binaries
echo "Copying binary files..."
mkdir -p ${DEST_DIR_BIN}
for FILE in ${BIN_FILES}; do
	if [ -f ${SRC_DIR_BIN}/${FILE} ]; then
		cp -f ${SRC_DIR_BIN}/${FILE} ${DEST_DIR_BIN}/${FILE}
	else
		echo "WARNING: ${SRC_DIR_BIN}/${FILE} does not exist"
	fi
done

# Data
echo "Copying data files..."
mkdir -p ${DEST_DIR_DATA}
for FILE in ${DATA_FILES}; do
	if [ -f ${SRC_DIR_DATA}/${FILE} ]; then
		cp -f ${SRC_DIR_DATA}/${FILE} ${DEST_DIR_DATA}/${FILE}
	else
		echo "WARNING: ${SRC_DIR_DATA}/${FILE} does not exist"
	fi
done

# Special
if [ "${PLATFORM_CHOICE}" = "linux" ]; then
	FILE=libg2c.so.0
	echo "Copying ${FILE}..."
	if [ -r "${DEST_DIR}/${FILE}" ]; then
		cp ${DEST_DIR}/${FILE} ${DEST_DIR_BIN}/${FILE}
	else
		echot "WARNING: ${DEST_DIR}/${FILE} does not exist"
	fi
fi

# Compress & cleanup
echo "Compressing..."
cd ${DEST_DIR_PLAT} && \
	zip -rq ../adde-${PLATFORM_CHOICE}.zip adde && \
	cd - >/dev/null && \
	rm -Rf ${DEST_DIR_PLAT}
