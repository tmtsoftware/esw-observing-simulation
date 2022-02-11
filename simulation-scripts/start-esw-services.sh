#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
ESW_VERSION=0.4.0
SEQ_SCRIPT_VERSION=d522f5c
cs launch esw-services:$ESW_VERSION -- start-eng-ui-services  --scripts-version $SEQ_SCRIPT_VERSION --obs-mode-config $ROOT/../sample-configs/smObsModeConfig.conf
