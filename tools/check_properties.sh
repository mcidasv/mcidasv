#!/bin/sh

IDVP=../../IDV/ucar/unidata/idv/resources/idv.properties
MCVP=../../mcidasv/edu/wisc/ssec/mcidasv/resources/mcidasv.properties

cat ${IDVP} |sed -e 's/#.*//' -e 's/=.*//' |sort -n |uniq >/tmp/idv.properties
cat ${MCVP} |sed -e 's/#.*//' -e 's/=.*//' |sort -n |uniq >/tmp/mcidasv.properties

diff /tmp/idv.properties /tmp/mcidasv.properties
