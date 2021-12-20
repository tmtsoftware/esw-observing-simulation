#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT/../../../target

V_SLICE_ZIP=https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v0.4/tcs-vslice-04.zip
CONTAINER_CONF_PATH=$1

if [ -d "tcs-vslice-04" ]
then
    echo "starting tcs-vslice container"
else
   curl -L -O $V_SLICE_ZIP
   unzip -o tcs-vslice-04
fi

tcs-vslice-04/bin/tcs-deploy --local $CONTAINER_CONF_PATH