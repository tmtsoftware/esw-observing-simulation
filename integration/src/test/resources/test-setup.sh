#!/usr/bin/env bash

if [[ "$OSTYPE" == "darwin"* ]]; then
  export INTERFACE_NAME=en0
  export AAS_INTERFACE_NAME=en0
else
  export INTERFACE_NAME=eth0
  export AAS_INTERFACE_NAME=eth0
fi

echo "INTERFACE_NAME:"$INTERFACE_NAME
echo "AAS_INTERFACE_NAME:"$AAS_INTERFACE_NAME

ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

if [[ "$OSTYPE" == "darwin"* ]]; then

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

else
  echo $ROOT
  cd $ROOT/../../../../../tcs-vslice-0.4
  echo "after switching to tcs vslice folder"
  echo pwd
  ./install/tcs-vslice-04/bin/tcs-deploy --local $CONTAINER_CONF_PATH
fi