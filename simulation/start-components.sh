#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"

export INTERFACE_NAME=en0
export TMT_LOG_HOME=/tmp/tmt
sampleConfDir=$ROOT/../sample-configs
hostConfPath=$sampleConfDir/HostConfig.conf
irisContainerPath=$sampleConfDir/IrisContainer.conf
wfosContainerPath=$sampleConfDir/WfosContainer.conf
ESW_VERSION=2c76965

awk  -v searchStr="irisContainerPath" -v replaceStr="$irisContainerPath" '{sub(searchStr,replaceStr); print;}' $hostConfPath > tempfile && mv tempfile $hostConfPath
awk  -v searchStr="wfosContainerPath" -v replaceStr="$wfosContainerPath" '{sub(searchStr,replaceStr); print;}' $hostConfPath > tempfile && mv tempfile $hostConfPath
cs launch esw-agent-akka-app:$ESW_VERSION -- start -p "iris.machine99" -l --host-config-path $hostConfPath