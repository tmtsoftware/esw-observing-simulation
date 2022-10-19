#!/usr/bin/env bash
version=$1

if [[ "$OSTYPE" == "darwin"* ]]; then
  #Please do not remove below , as this zip is used when we run tcs assembly on Mac
  V_SLICE_ZIP="https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v$version/tcs-vslice-$version.zip"
elif [ "$USER" == "jenkins" ]; then
  # this zip is used when we run tcs assembly on BTE
  V_SLICE_ZIP="https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v$version/tcs-vslice-$version-CentOS-7.zip"
else
  # this zip is used when we run tcs assembly on ubuntu
  V_SLICE_ZIP="https://github.com/tmtsoftware/tcs-vslice-0.4/releases/download/v$version/tcs-vslice-$version-Ubuntu-20.04.zip"
fi

TCS_VSLICE="$HOME/tcs-vslice-04"

rm -rf "$TCS_VSLICE/$version"
mkdir -p "$TCS_VSLICE/$version"
echo "downloading.." $V_SLICE_ZIP
curl -L $V_SLICE_ZIP -o "tcs-vslice-04.zip"
unzip -o "tcs-vslice-04.zip" -d "$TCS_VSLICE/$version"
