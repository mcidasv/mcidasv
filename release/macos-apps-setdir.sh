#!/bin/sh

if [ -n "$1" ]; then

	FILE="Contents/MacOS/run_script"
	export NEW_DIR=`echo "$1" |/usr/bin/sed -e 's/\//\\\\\//g'`

	# Run shortcut
	APP="Run McIDAS-V.app"
	/bin/ed "${APP}/${FILE}" <<-EOF >/dev/null 2>&1
		/^INSTALL_DIR=/
		s/INSTALL_DIR=.*/INSTALL_DIR="${NEW_DIR}"/
		.
		w
		q
	EOF

	# Configure shortcut
	APP="Configure McIDAS-V.app"
	/bin/ed "${APP}/${FILE}" <<-EOF >/dev/null 2>&1
		/^INSTALL_DIR=/
		s/INSTALL_DIR=.*/INSTALL_DIR="${NEW_DIR}"/
		.
		w
		q
	EOF

fi
