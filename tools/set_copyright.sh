#
# $Id$
#

HEADER="copyright.header"
DIRS="edu ucar"

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
		COPYRIGHT=$(grep -c " * Copyright" "${FILE}")
		if [ ${COPYRIGHT} -ne 0 ]; then
			echo "  ${FILE} is copyrighted!"
		else
			echo "  Updating ${FILE}"
#			cat "${HEADER}" "${FILE}" > "${FILE}.copyright"
#			mv "${FILE}.copyright" "${FILE}"
		fi

	done

done
