#!/bin/sh

# tidy -wrap 0 -i -q -omit

for FILE in `find . -name "*.html"`; do
	OUTPUT=`tidy -o /tmp/tidy.html -f test.out ${FILE}`;
	if [ "${OUTPUT}" != "" ]; then
		echo "- ${FILE}:"
		echo "${OUTPUT}"
	fi
done
