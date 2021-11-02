#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

export INTERFACE_NAME=en0
export TMT_LOG_HOME=/tmp/tmt

cs launch esw-agent-akka-app:2c76965 -- start -p "iris.machine99" -l --host-config-path "$ROOT/../sample-configs/HostConfig.conf"