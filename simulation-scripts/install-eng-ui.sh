#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT
source ./versions.sh
version=$ENG_UI_VERSION

set -x
cd $ROOT/../apps

rm -f *.txt
rm -f *.zip

ENG_UI_HOME="$ROOT/../apps/"

echo "downloading.. esw-ocs-eng-ui with $version"
curl -L -O https://github.com/tmtsoftware/esw-ocs-eng-ui/releases/download/v$version/esw-ocs-eng-ui.zip

rm -rf esw-ocs-eng-ui
unzip -o esw-ocs-eng-ui.zip
touch $version.txt
