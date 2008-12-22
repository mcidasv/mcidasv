#
# $Id$
#

HEADER="copyright.header"
DIRS="edu ucar"
CONTYN="n"

# Check for HEADER existance
if [ ! -r "${HEADER}" ]; then
	echo "Header file \"${HEADER}\" not found"
	exit 1
fi

# Find .java files in each DIRS
for DIR in ${DIRS}; do

	# Get list of all source files
	echo "Looking in ../${DIR}"
	FILES=$(find ../${DIR} -name "*.java")
	for FILE in $FILES; do

		# Check for existance of Copyright
		COPYRIGHT_SSEC=$(grep -c " * This file is part of McIDAS-V" "${FILE}")
		COPYRIGHT_UNIDATA=$(grep -c " * support@unidata.ucar.edu" "${FILE}")

		if [ ${COPYRIGHT_UNIDATA} -ne 0 ]; then
			echo "! ${FILE} is copyrighted by Unidata!"
			continue
		fi

		if [ ${COPYRIGHT_SSEC} -ne 0 ]; then
SKIP=1
#			echo "  ${FILE} is copyrighted!"
		else
			if [ "${CONTYN}" != "a" -a "${CONTYN}" != "A" ]; then
				echo -n "  Update ${FILE}? [y/N]"
				read CONTYN
			fi
			if [ "${CONTYN}" = "y" -o "${CONTYN}" = "Y" -o "${CONTYN}" = "a" -o "${CONTYN}" = "A" ]; then
				echo "  Updating ${FILE}"
				cat "${HEADER}" "${FILE}" > "${FILE}.copyright"
				mv "${FILE}.copyright" "${FILE}"
			fi
		fi

	done

done
