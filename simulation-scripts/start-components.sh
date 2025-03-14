#!/usr/bin/env bash
ROOT="$(
    cd "$(dirname "$0")" >/dev/null 2>&1 || exit
    pwd -P
)"
cd $ROOT
source ./versions.sh

sampleConfDir=$ROOT/../sample-configs
hostConfPath=$sampleConfDir/HostConfig.conf
irisContainerPath=$sampleConfDir/IrisContainer.conf
wfosContainerPath=$sampleConfDir/WfosContainer.conf
mkdir -p "$ROOT"/../target/
tempConfPath=$ROOT/../target/HostConfig.conf

cp "$hostConfPath" "$tempConfPath"

awk  -v searchStr="irisContainerPath" -v replaceStr="$irisContainerPath" '{sub(searchStr,replaceStr); print;}' "$tempConfPath" > tempfile && mv tempfile "$tempConfPath"
awk  -v searchStr="wfosContainerPath" -v replaceStr="$wfosContainerPath" '{sub(searchStr,replaceStr); print;}' "$tempConfPath" > tempfile && mv tempfile "$tempConfPath"
set -x
cs launch --channel $CHANNEL esw-agent-pekko-app:$ESW_VERSION -- start -p "iris.machine99" -l --host-config-path "$tempConfPath"
