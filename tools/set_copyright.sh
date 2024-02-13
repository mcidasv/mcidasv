#!/usr/bin/env bash

COPYRIGHT="../release/licenses/COPYRIGHT"
HEADER="/tmp/copyright.header"

DIRS="edu ucar visad release/install4j-custom-code"
MAX_CLIP=30

TEMP="/tmp/copyright.temp"
CONTYN="n"

# Make COPYRIGHT into Java-style comment
rm -f "${HEADER}"
echo "/*" >> "${HEADER}"
#echo " * \$Id\$" >> "${HEADER}"
#echo " *" >> "${HEADER}"
echo " * This file is part of McIDAS-V" >> "${HEADER}"
echo " *" >> "${HEADER}"
awk '{print " * " $0}' "${COPYRIGHT}" >> "${HEADER}"
echo " */" >> "${HEADER}"

HEADER_LINES=$(wc -l "${HEADER}" |awk '{print $1}')

# Find .java files in each DIRS
for DIR in ${DIRS}; do

	# Get list of all source files
	echo "Looking in ../${DIR}"
	FILES=$(find ../${DIR} -name "*.java")
	for FILE in $FILES; do

		# Check for exact SSEC copyright
		COPYRIGHT_CHECK=$(head -${HEADER_LINES} "${FILE}" |\
			sed -e 's/ \* \$Id.*/ \* \$Id\$/' > "${TEMP}" &&\
				diff --brief "${HEADER}" "${TEMP}")

		if [ -z "${COPYRIGHT_CHECK}" ]; then
SKIP=1
#			echo "  ${FILE} is copyrighted!"

		else
			if [ "${CONTYN}" != "a" -a "${CONTYN}" != "A" ]; then
				echo -n "  Update ${FILE}? [y/N] "
				read CONTYN
			fi
			if [ "${CONTYN}" = "y" -o "${CONTYN}" = "Y" -o "${CONTYN}" = "a" -o "${CONTYN}" = "A" ]; then
				echo "  Updating ${FILE}"

				# 1) Look for /* in first line, then look for first occurence of */
				#    Remove these lines
				START=$(grep -n "/\*" "${FILE}" |head -1 |awk -F: '{print $1}')
				if [ -n "${START}" -a ${START} -eq 1 ]; then
					END=$(grep -n " \*/" "${FILE}" |head -1 |awk -F: '{print $1}')
					if [ ${END} -gt ${MAX_CLIP} ]; then
						echo "    Not updating ${FILE}... would clip >${MAX_CLIP} (${END}) lines"
						continue
					fi
					echo "    Clipping ${END} comment lines from head of ${FILE}"
					LINES=$(wc -l "${FILE}" |awk '{print $1}')
					TAIL=$((${LINES} - ${END}))
					tail -${TAIL} "${FILE}" >"${FILE}.tail" && mv "${FILE}.tail" "${FILE}"
				fi

				# 2) Cat the header onto the file
				cat "${HEADER}" "${FILE}" > "${FILE}.copyright"
				mv "${FILE}.copyright" "${FILE}"

			fi
		fi

	done

done

rm "${HEADER}"
rm "${TEMP}"
