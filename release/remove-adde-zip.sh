#!/bin/sh

if [ -n "$1" ]; then

	DIR="$1"
	PLATS="darwin86 darwin linux solaris soli86 windows"

	for PLAT in ${PLATS}; do
		FILE="${DIR}/adde-${PLAT}.zip"
		if [ -f "${FILE}" ]; then
			rm -f "${FILE}"
		fi
	done

fi
