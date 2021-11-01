#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cs launch esw-services:abe11d9 -- start-eng-ui-services --esw-version abe11d9 --scripts-version 0.1.0-SNAPSHOT --obs-mode-config $ROOT/sample-configs/smObsModeConfig.conf