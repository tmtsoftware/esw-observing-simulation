#!/usr/bin/env bash

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
   sh $ROOT/install-tcs-assemblies.sh
fi

tcs-vslice-04/bin/tcs-deploy --local tcs-vslice-04/conf/McsEncPkContainer.conf
