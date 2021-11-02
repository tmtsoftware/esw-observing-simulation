#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT/../apps
curl -L -O https://github.com/tmtsoftware/esw-ocs-eng-ui/releases/download/v0.1.0/esw-ocs-eng-ui.zip
unzip -o esw-ocs-eng-ui.zip
