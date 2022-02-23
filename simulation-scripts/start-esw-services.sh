#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
ESW_VERSION=ab0f202
SEQ_SCRIPT_VERSION=fe74931
cs launch esw-services:$ESW_VERSION -- start-eng-ui-services --scripts-version $SEQ_SCRIPT_VERSION --obs-mode-config $ROOT/../sample-configs/smObsModeConfig.conf
