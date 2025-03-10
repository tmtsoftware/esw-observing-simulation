#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT
source ./versions.sh
version=$ENG_UI_VERSION

cd $ROOT/../apps

rm -r *.txt
rm -r *.zip

ENG_UI_HOME="$ROOT/../apps/"

echo "downloading.. esw-ocs-eng-ui with $version"
curl -L -O https://github.com/tmtsoftware/esw-ocs-eng-ui/archive/$version.zip

unzip -o $version.zip
cd esw-ocs-eng-ui-$version* && npm install && npm run build
rm -rf ../esw-ocs-eng-ui
mv esw-ocs-eng-ui ../esw-ocs-eng-ui
cd ..
rm -rf esw-ocs-eng-ui-$version*
touch $version.txt
