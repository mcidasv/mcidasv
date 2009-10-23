#!/bin/bash

VERSION=$(/usr/bin/java -version 2>&1 |/usr/bin/head -1 |/usr/bin/sed -e 's/.*"\(...\).*/\1/')

COMPARE=`echo "${VERSION} > 1.5" |/usr/bin/bc`

exit ${COMPARE}
