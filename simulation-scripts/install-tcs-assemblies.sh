#!/usr/bin/env bash

if [[ "$OSTYPE" == "darwin"* ]]; then
  #Please do not remove below , as this zip is used when we run tcs assembly on Mac
  V_SLICE_ZIP=https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v0.6/tcs-vslice-0.6.zip
else
  # this zip is used when we run tcs assembly on ubuntu
  V_SLICE_ZIP=https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v0.6/tcs-vslice-06-Ubuntu-20.04.zip
fi

ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

cd $ROOT/../

rm -rf tcs-vslice-04
echo "downloading.." $V_SLICE_ZIP
curl -L $V_SLICE_ZIP -o tcs-vslice-04.zip
unzip -o tcs-vslice-04.zip
