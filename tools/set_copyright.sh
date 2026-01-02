#!/usr/bin/env bash

# Slightly modified version of the copyright.sh in the VisAD repo.
# the only caveat is that we'll have to require that each file
# has the LGPL header, otherwise this won't work.

NEW_COPYRIGHT_RANGE="2007-2026"

SRC_DIRS="edu ucar visad release/install4j-custom-code"

cd $(dirname $0)/..
if [[ "$OSTYPE" == "darwin"* ]]; then
  find $SRC_DIRS -name '*.java' -print0 | xargs -0 \
    sed -i '' "s/Copyright 2007-\([0-9]\{4\}\)/Copyright $NEW_COPYRIGHT_RANGE/"
else
  find $SRC_DIRS -name '*.java' -print0 | xargs -0 \
    sed -i'' -e "s/Copyright 2007-\([0-9]\{4\}\)/Copyright $NEW_COPYRIGHT_RANGE/"
fi

