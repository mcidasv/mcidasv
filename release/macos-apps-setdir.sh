#!/bin/sh

if [ -n "$1" ]; then

	FILE="Contents/MacOS/run_script"
	TOP_DIR=`echo "$1" |/usr/bin/sed -e 's/\//\\\\\//g'`
	export APP_DIR="${TOP_DIR}"

	# Run shortcut
	APP="/Applications/McIDAS-V Launcher.app"
	EXEC_FILE="${APP}/Contents/MacOS/run_script"
	/bin/ed "${APP}/${FILE}" <<-EOF >/dev/null 2>&1
		/^INSTALL_DIR=/
		s/INSTALL_DIR=.*/INSTALL_DIR="${APP_DIR}"/
		.
		w
		q
	EOF
	/bin/chmod 755 "${EXEC_FILE}"

	# Configure shortcut
	APP="/Applications/McIDAS-V Configuration.app"
	EXEC_FILE="${APP}/Contents/MacOS/run_script"
	/bin/ed "${APP}/${FILE}" <<-EOF >/dev/null 2>&1
		/^INSTALL_DIR=/
		s/INSTALL_DIR=.*/INSTALL_DIR="${APP_DIR}"/
		.
		w
		q
	EOF
	/bin/chmod 755 "${EXEC_FILE}"

fi
