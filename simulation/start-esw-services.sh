#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cs launch esw-services:2c76965 -- start-eng-ui-services --scripts-version aad9d5a --obs-mode-config $ROOT/../sample-configs/smObsModeConfig.conf