#!/bin/sh

if [ -n "$1" ]; then

	FILE="Contents/MacOS/run_script"
	export APP_DIR=`echo "$1" |/usr/bin/sed -e 's/\//\\\\\//g'`

	# Run shortcut
	APP="./McIDAS-V Launcher.app"
	/bin/ed "${APP}/${FILE}" <<-EOF >/dev/null 2>&1
		/^INSTALL_DIR=/
		s/INSTALL_DIR=.*/INSTALL_DIR="${APP_DIR}"/
		.
		w
		q
	EOF
	/bin/chmod 755 "${APP}/${FILE}"

	# Configure shortcut
	APP="./McIDAS-V Configuration.app"
	/bin/ed "${APP}/${FILE}" <<-EOF >/dev/null 2>&1
		/^INSTALL_DIR=/
		s/INSTALL_DIR=.*/INSTALL_DIR="${APP_DIR}"/
		.
		w
		q
	EOF
	/bin/chmod 755 "${APP}/${FILE}"

fi
