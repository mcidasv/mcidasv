#
# $Id$
#

HEADER="copyright.header"
TEMP="/tmp/copyright.temp"
CHECK="/tmp/copyright.check"
DIRS="edu ucar"
CONTYN="n"

# Check for HEADER existance
if [ ! -r "${HEADER}" ]; then
	echo "Header file \"${HEADER}\" not found"
	exit 1
fi

# Make diff-able header
sed -e 's/ \* \$Id.*/ \* \$Id\$/' "${HEADER}" > "${CHECK}"

# Find .java files in each DIRS
for DIR in ${DIRS}; do

	# Get list of all source files
	echo "Looking in ../${DIR}"
	FILES=$(find ../${DIR} -name "*.java")
	for FILE in $FILES; do

		# Check for existance of Unidata copyright
		COPYRIGHT_UNIDATA=$(grep -c " * support@unidata.ucar.edu" "${FILE}")
		if [ ${COPYRIGHT_UNIDATA} -ne 0 ]; then
			echo "! ${FILE} is copyrighted by Unidata!"
			continue
		fi

		# Check for exact SSEC copyright
		COPYRIGHT_LINES=$(wc -l "${HEADER}" |awk '{print $1}')
		COPYRIGHT_CHECK=$(head -${COPYRIGHT_LINES} "${FILE}" |\
			sed -e 's/ \* \$Id.*/ \* \$Id\$/' > "${TEMP}" &&\
				diff --brief "${CHECK}" "${TEMP}")
		if [ -z "${COPYRIGHT_CHECK}" ]; then
SKIP=1
#			echo "  ${FILE} is copyrighted!"
		else
			COMMENT=$(head -${COPYRIGHT_LINES} "${FILE}" |grep "/\*")
			if [ -n "${COMMENT}" ]; then
				echo "! ${FILE} needs to be updated manually"
				continue
			fi
			if [ "${CONTYN}" != "a" -a "${CONTYN}" != "A" ]; then
				echo -n "  Update ${FILE}? [y/N] "
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

#rm "${CHECK}"
#rm "${TEMP}"
