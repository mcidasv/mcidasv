#!/bin/sh

IDVP=../../idv/ucar/unidata/idv/resources/idv.properties
MCVP=../../mc-v/edu/wisc/ssec/mcidasv/resources/mcidasv.properties

cat ${IDVP} |sed -e 's/#.*//' -e 's/=.*//' |sort -n |uniq >/tmp/idv.properties
cat ${MCVP} |sed -e 's/#.*//' -e 's/=.*//' |sort -n |uniq >/tmp/mcidasv.properties

diff /tmp/idv.properties /tmp/mcidasv.properties
