#!/bin/sh

if [ -n "$1" ]; then

	DIR="$1"

	# If one of the adde-platform.zip files exists, delete it
	PLATS="darwin86 darwin linux solaris soli86 windows"
	for PLAT in ${PLATS}; do
		FILE="${DIR}/adde-${PLAT}.zip"
		if [ -f "${FILE}" ]; then
			rm -f "${FILE}"
		fi
	done

	# Remove the macos-specific files if they exist
	if [ -f "${DIR}/macos-java3d_jogl.zip" ]; then
		rm -f "${DIR}/macos-java3d_jogl.zip"
	fi
	if [ -f "${DIR}/macos-apps.zip" ]; then
		rm -f "${DIR}/macos-apps.zip"
	fi

fi
