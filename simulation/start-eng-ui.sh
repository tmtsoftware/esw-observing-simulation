#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT/../apps
if [ -d "esw-ocs-eng-ui" ]
then
    echo "Serving esw-ocs-eng-ui app "
else
    echo "Error: Directory esw-ocs-eng-ui does not exists. downloading..."
    sh $ROOT/install-eng-ui.sh
fi
./serve.py