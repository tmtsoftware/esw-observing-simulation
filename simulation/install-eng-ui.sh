#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT/../apps

# use this for installing released version
#curl -L -O https://github.com/tmtsoftware/esw-ocs-eng-ui/releases/download/v0.1.0/esw-ocs-eng-ui.zip
#unzip -o esw-ocs-eng-ui.zip


## use following for installing latest eng-ui
curl -L -O https://github.com/tmtsoftware/esw-ocs-eng-ui/archive/refs/heads/main.zip
unzip -o main.zip
cd esw-ocs-eng-ui-main && npm install && npm run build
mv esw-ocs-eng-ui ../esw-ocs-eng-ui
cd ..
rm -rf esw-ocs-eng-ui-main
