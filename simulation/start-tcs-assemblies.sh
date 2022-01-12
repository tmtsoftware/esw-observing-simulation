#!/usr/bin/env bash

if [[ "$OSTYPE" == "darwin"* ]]; then
  export INTERFACE_NAME=en0
else
  test "$INTERFACE_NAME" || export INTERFACE_NAME=eth0
fi
V_SLICE_ZIP=https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v0.5/tcs-vslice-05-Ubuntu-20.04.zip

echo "Setting INTERFACE_NAME tcs assemblies:"$INTERFACE_NAME

export TPK_USE_FAKE_SYSTEM_CLOCK=1

ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

cd $ROOT/../

if [ -d "tcs-vslice-04" ]
then
    echo "starting tcs-vslice container"
else
   echo "downloading.." $V_SLICE_ZIP
   curl -L $V_SLICE_ZIP -o tcs-vslice-04.zip
   unzip -o tcs-vslice-04.zip
fi

tcs-vslice-04/bin/tcs-deploy --local tcs-vslice-04/conf/McsEncPkContainer.conf
