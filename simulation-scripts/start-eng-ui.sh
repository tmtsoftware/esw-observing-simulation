#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT
source ./versions.sh
version=$ENG_UI_VERSION
cd $ROOT/../apps

if [ -f "$version.txt" ]
then
    echo "Serving esw-ocs-eng-ui app "
else
    echo "Error: Directory esw-ocs-eng-ui does not exists. downloading..."
    sh $ROOT/install-eng-ui.sh
fi
./serve.py
