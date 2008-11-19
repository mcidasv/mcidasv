#!/bin/sh

for FILE in `find . -name "*.html"`; do
	LINK=`echo "${FILE}" |sed -e 's/^\.\///' -e 's/\//\\\\\\//g`
	echo $FILE: $LINK
	dos2unix "${FILE}" |sed -e s/CURRENTFILE/${LINK}/g > "${FILE}.tmp" && mv "${FILE}.tmp" "${FILE}"
done
