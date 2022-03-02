#!/usr/bin/env bash
export TPK_USE_FAKE_SYSTEM_CLOCK=1

ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT
source ./versions.sh

version=$TCS_VERSION
TCS_VSLICE="$HOME/tcs-vslice-04/$version"

if [ -d $TCS_VSLICE ]
then
    echo "starting tcs-vslice container"
else  
   "$ROOT/install-tcs-assemblies.sh" "$version"
fi

"$TCS_VSLICE/tcs-vslice-04/bin/tcs-deploy" --local "$TCS_VSLICE/tcs-vslice-04/conf/McsEncPkContainer.conf"
